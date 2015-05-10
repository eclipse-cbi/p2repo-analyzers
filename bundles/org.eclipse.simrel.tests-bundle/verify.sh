#!/usr/bin/env bash

if [[ $# != 2 && $# != 3 ]] 
then
    printf "\tERROR: Wrong number of arguments. This script, "${0##*/}", requires the name of the jar or jar.pack.gz file to verify and the version of Java to use 8,7,6, or 5, and optionally the location scanned.\n" >&2
    exit 1
fi

if [[ -z $VERIFYOUTDIR ]] 
then
    printf "\tERROR: this script, "${0##*/}", requires VERIFYOUTDIR to be defined.\n" >&2
    exit 1
fi



filename="${1}"

if [ ! -e "${filename}" ]
then
    printf "\tERROR: file does not exist:" "${filename}\n" >&2
    exit 1
fi

JAVA_VER=$2

if [[ ! "${JAVA_VER}" =~ [5,6,7,8] ]]
then
    printf "\tERROR: The second argumnet to this script,  "${0##*/}", must be a Java Version numerial, from 5 to 8\n" >&2
fi

#Location is optional. Just used to write in info file.
LOCATION=$3

jarname=$(basename "${filename}")

JAVA_5_HOME=/shared/common/jdk1.5.0-latest
JAVA_6_HOME=/shared/common/jdk1.6.0-latest
JAVA_7_HOME=/shared/common/jdk1.7.0-latest
JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest

case $JAVA_VER in
    5) 
    JAVA_HOME=$JAVA_5_HOME
    ;;
    6)
    JAVA_HOME=$JAVA_6_HOME
    ;;
    7)
    JAVA_HOME=$JAVA_7_HOME
    ;;
    8)
    JAVA_HOME=$JAVA_8_HOME
    ;;
    *)
    printf "\n\tPROGRAM ERROR: \t%s\n" "JAVA_VER, ${JAVA_VER}, was not valid (which was checked earlier in regex expression)"
    ;;
esac

INFO_FILENAME="info${JAVA_VER}.txt"
INFO_FILE="${VERIFYOUTDIR}/${INFO_FILENAME}"
# if already exists, we don't need to re-write
if [[ ! -e "${INFO_FILE}" ]]
then
    printf "JAVA_HOME: \t%s\n\n" "${JAVA_HOME}" > "${INFO_FILE}"
    printf "Java version: \t%s\n\n" "$( ${JAVA_HOME}/bin/java -version 2>&1 )" >> "${INFO_FILE}"
    printf "verify script: \t%s\n\n" "${0}" >> "${INFO_FILE}"
    printf "location scanned: \t${LOCATION}" >> "${INFO_FILE}" 
fi 

if [ ! -e "${JAVA_HOME}" ]
then
    printf "\tERROR: this script, "${0##*/}", requires JAVA_HOME to be defined and exist.\n" >&2
    exit 1
fi

UNPACK200_EXE=$JAVA_HOME/jre/bin/unpack200
VERIFY_EXE=$JAVA_HOME/bin/jarsigner
#VERIFY_OPTIONS="-verify -verbose  -certs"
#COMPACT=false
#VERIFY_OPTIONS="-verify -verbose"
VERIFY_OPTIONS="-verify"
COMPACT=true

CHECK_SUM_FILE="${VERIFYOUTDIR}/checksums${JAVA_VER}.txt"
VERIFIED_OUTFILE="${VERIFYOUTDIR}/verified${JAVA_VER}.txt"
KNOWN_EXCEPTION="${VERIFYOUTDIR}/knownunsigned${JAVA_VER}.txt"
UNSIGNED_OUTFILE="${VERIFYOUTDIR}/unsigned${JAVA_VER}.txt"
NOMANIFEST="${VERIFYOUTDIR}/nomanifest${JAVA_VER}.txt"
ERROR_EXIT_FILE="${VERIFYOUTDIR}/error${JAVA_VER}.txt"
NESTED_JARS="${VERIFYOUTDIR}/nestedjars${JAVA_VER}.txt"
TMP_DIR=${TMP_DIR:-/shared/orbit/tmp/}
mkdir -p ${TMP_DIR}
PPAT_PACKGZ="(.*).pack.gz$"
if [[ "$jarname" =~  $PPAT_PACKGZ ]]
then 
    basejarname=${BASH_REMATCH[1]}
    #echo -e "\n basejarname: " $basejarname "\n"
    "${UNPACK200_EXE}" $filename ${TMP_DIR}/$basejarname
    vresult=`"${VERIFY_EXE}" ${VERIFY_OPTIONS} ${TMP_DIR}/$basejarname`
    exitcode=$?
    #nestedPackedJars=$( ${JAVA_HOME}/bin/jar -t ${TMP_DIR}/$basejarname | grep "pack.gz" )
    nestedPackedJars=$(  unzip -t ${TMP_DIR}/$basejarname 2>/dev/null | grep "pack.gz" )
    if [[ -n $nestedPackedJars ]]
    then
        echo "$filename contains nested packed jars" >> ${NESTED_JARS}
        echo "$nestedPackedJars" >> ${NESTED_JARS}
    fi
    csum=$(md5sum "${TMP_DIR}/${basejarname}")
    # echo -e "$basejarname  \t${csum%% *}  \tjar.pack.gz" >> "${CHECK_SUM_FILE}"
    printable_basejarname=$basejarname
    printable_checksum=${csum%% *}
    printable_type=jar.pack.gz
    rm ${TMP_DIR}/$basejarname
else
    vresult=`"${VERIFY_EXE}" ${VERIFY_OPTIONS} $filename`
    exitcode=$?
    csum=$(md5sum "$filename")
    # echo -e "${filename##*/} \t${csum%% *} \tjar" >> "${CHECK_SUM_FILE}"
    printable_basejarname=${filename##*/}
    printable_checksum=${csum%% *}
    printable_type=jar
fi

# jarsigner sometimes returns one line, sometimes two ... we take 
# out EOLs to print compactly
if [[ "true" == "${COMPACT}" ]]
then
    vresultoneline=$(echo "${vresult}" | tr '\n' ' ' | tr '\r' ' ')
else
    vresultoneline="${vresult}"
fi
#vresultoneline=$(echo "${vresult}" | tr '\n' ' ' | tr '\r' ' ')
# known response patterns from jarsigner (assumes EOLs removed from string)
PPAT_VERIFIED="^.*jar\ verified.*"
# no manifest is not signed for our purposes ... occurs a lot for unsigned feature jars
PPAT_UNSIGNED_OR_NOMANIFEST="^.*(jar is unsigned)|(no manifest).*"
# do not currently use unsigned or no manifest (by themselves) 
# nor "copy mode" ... copy mode printed to stdout by unpack200
#PPAT_UNSIGNED="^jar is unsigned.*"
#PPAT_NOMANIFEST="^no manifest.*"
#PPAT_COPYMODE="^Copy-mode\..*"

JAR_TYPE="unknown"
if [[ "${filename}" =~ .*plugins/.* ]]
then
    JAR_TYPE="plugin"
elif [[ "${filename}" =~ .*features/.* ]]
then
    JAR_TYPE="features"
else
    JAR_TYPE="loose jar?" 
fi


if [[ "${vresultoneline}" =~ $PPAT_VERIFIED ]]
then
    printf '%-80s \t%-15s \t\t' "${jarname}" "$JAR_TYPE" >> "${VERIFIED_OUTFILE}" 
    printf '%s\n' " ${vresultoneline} " >> "${VERIFIED_OUTFILE}"
    # write checksums for verified files
    printf "%-90s %30s %-15s\n" "${printable_basejarname}" $printable_checksum $printable_type >> "${CHECK_SUM_FILE}"
elif [[ "${vresultoneline}" =~ $PPAT_UNSIGNED_OR_NOMANIFEST ]]
then
    # list "known cases", that can not be signed, 
    # in their own "known exception file", else list in "unsigned" file.

    # For reasons of addition of org.eclipse.jdt.core.compiler.batch see
    # https://bugs.eclipse.org/bugs/show_bug.cgi?id=356382
    # For small pointer about commonj.sdo, see
    # https://bugs.eclipse.org/bugs/show_bug.cgi?id=276999
    PPAT_COMMON_UNSIGNED='(artifacts\.jar)|(content\.jar)|(compositeArtifacts.jar)|(compositeContent.jar)'
    PPAT_ORBIT_UNSIGNED='(commonj\.sdo.*)'
    PPAT_ECLIPSE_UNSIGNED='(org\.eclipse\.jdt\.core\.compiler\.batch.*)|(org\.eclipse\.jdt\.core\.compiler\.batch\.source_.*)'
    PPAT_KNOWN_UNSIGNED="$PPAT_COMMON_UNSIGNED|$PPAT_ORBIT_UNSIGNED|$PPAT_ECLIPSE_UNSIGNED"
    echo "INFO: KNOWN UNSIGNED: $PPAT_KNOWN_UNSIGNED"
    if  [[ ${jarname} =~ $PPAT_KNOWN_UNSIGNED ]]       
    then
        printf '%-80s \t%-15s \t\t' "${jarname}" "$JAR_TYPE" >> "${KNOWN_EXCEPTION}"
        printf '%s\n' " ${vresultoneline} "  >> "${KNOWN_EXCEPTION}"

    else
        # purposely no line delimiter, so output of jarsigner is on same line
        printf '%-80s \t%-15s \t\t' "${jarname}" "$JAR_TYPE" >> "${UNSIGNED_OUTFILE}" 
        printf '%s\n' " ${vresultoneline} "  >> "${UNSIGNED_OUTFILE}"
    fi 
else 
    # fall through if unexpected result. Will happen if can not unpack200 a file
    printf '%-80s \t%-15s \t\t' "${jarname}" "$JAR_TYPE" >> "${ERROR_EXIT_FILE}" 
    printf '%s\n' " ${vresultpneline} "  >> "${ERROR_EXIT_FILE}" 
fi

# When 'compact' thing sort well, but, not if non-compact output.
if [[ "true" == "${COMPACT}" ]] 
then

sort "${VERIFIED_OUTFILE}"
sort "${KNOWN_EXCEPTION}"
sort "${UNSIGNED_OUTFILE}"

fi

if [[ $exitcode -gt 0 ]]
then
    echo -e "\n exitcode: " $exitcode: $(basename $filename)" \n"  >> "${ERROR_EXIT_FILE}"  
fi

