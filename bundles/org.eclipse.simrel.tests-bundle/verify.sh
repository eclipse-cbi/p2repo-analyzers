#!/usr/bin/env bash

if [[ $# != 2 ]]
then
    printf "\tERROR: this script, "${0##*/}", requires the name of the jar or jar.pack.gz file to verify and the version of Java to use 8,7,6, or 5.\n" >&2
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

jarname=$(basename "${filename}")

# Best to have a number of "JAVA_N_HOME" defined, since one argument
# to this script is "JAVA_VER" which allows the calling script so specify
# which version to use for verification.
JAVA_5_HOME=/shared/common/jdk1.5.0-latest
JAVA_6_HOME=/shared/common/jdk1.6.0-latest
JAVA_7_HOME=/shared/common/jdk1.7.0-latest
JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
# We always set JAVA_HOME explicitly to what we want, since on many systems,
# is it set to some JRE that would not suffice.

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
    printf "JAVA_HOME: \t%s\n\n" "${JAVA_HOME}" > "${VERIFYOUTDIR}/${INFO_FILENAME}"
    printf "Java version: \t%s\n\n" "$( ${JAVA_HOME}/bin/java -version 2>&1 )" >> "${VERIFYOUTDIR}/${INFO_FILENAME}"
    printf "verify script: \t%s\n\n" "${0}" >> "${VERIFYOUTDIR}/${INFO_FILENAME}"

    if [ ! -e "${JAVA_HOME}" ]
    then
        printf "\tERROR: this script, "${0##*/}", requires JAVA_HOME to be defined and exist.\n" >&2
        exit 1
    fi

    UNPACK200_EXE=$JAVA_HOME/jre/bin/unpack200
    VERIFY_EXE=$JAVA_HOME/bin/jarsigner
    #VERIFY_OPTIONS="-verify -verbose  -certs"
    #VERIFY_OPTIONS="-verify -verbose"
    VERIFY_OPTIONS="-verify"

    VERIFIED_OUTFILE="${VERIFYOUTDIR}/verified${JAVA_VER}.txt"
    KNOWN_EXCEPTION="${VERIFYOUTDIR}/knownunsigned${JAVA_VER}.txt"
    UNSIGNED_OUTFILE="${VERIFYOUTDIR}/unsigned${JAVA_VER}.txt"
    NOMANIFEST="${VERIFYOUTDIR}/nomanifest${JAVA_VER}.txt"
    ERROR_EXIT_FILE="${VERIFYOUTDIR}/error${JAVA_VER}.txt"
    NESTED_JARS="${VERIFYOUTDIR}/nestedjars${JAVA_VER}.txt"
    TMP_DIR=${TMP_DIR:-/shared/orbit/tmp}
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
        nestedPackedJars=$(  unzip -t ${TMP_DIR}/$basejarname | grep "pack.gz" )
        if [[ -n $nestedPackedJars ]]
        then
            echo "$filename contains nested packed jars" >> ${NESTED_JARS}
            echo "$nestedPackedJars" >> ${NESTED_JARS}
        fi
        rm ${TMP_DIR}/$basejarname
    else
        vresult=`"${VERIFY_EXE}" ${VERIFY_OPTIONS} $filename`
        exitcode=$?
    fi

    # jarsigner sometimes returns one line, sometimes two ... we take
    # out EOLs to print compactly
    vresultoneline=`echo "${vresult}" | tr '\n' ' '`

    # known response patterns from jarsigner (assumes EOLs removed from string)
    PPAT_VERIFIED="^.*jar\ verified.*"
    # no manifest is not signed for our purposes ... occurs a lot for unsigned feature jars
    PPAT_UNSIGNED_OR_NOMANIFEST="^.*(jar is unsigned)|(no manifest).*"
    # do not currently use unsigned or no manifest (by themselves)
    # nor "copy mode" ... copy mode printed to stdout by unpack200
    #PPAT_UNSIGNED="^jar is unsigned.*"
    #PPAT_NOMANIFEST="^no manifest.*"
    #PPAT_COPYMODE="^Copy-mode\..*"

    if [[ "${vresultoneline}" =~ $PPAT_VERIFIED ]]
    then
        printf '%-80s \t\t' "   ${jarname}: " >> "${VERIFIED_OUTFILE}"
        printf '%s\n' " ${vresult} " >> "${VERIFIED_OUTFILE}"
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
        echo "KNOWN UNSIGNED: $PPAT_KNOWN_UNSIGNED" >&2
        if  [[ ${jarname} =~ $PPAT_KNOWN_UNSIGNED ]]
        then
            printf '%-100s \t\t' "   ${jarname}: "  >> "${KNOWN_EXCEPTION}"
            printf '%s\n' " ${vresult} "  >> "${KNOWN_EXCEPTION}"

        else
            # purposely no line delimiter, so output of jarsigner is on same line
            printf '%-100s \t\t' "   ${jarname}: "  >> "${UNSIGNED_OUTFILE}"
            printf '%s\n' " ${vresult} "  >> "${UNSIGNED_OUTFILE}"
        fi

    else
        # fall through if unexpected result. Will happen if can not unpack200 a file
        printf '%-100s \t\t' "   ${jarname}: "  >> "${ERROR_EXIT_FILE}"
        printf '%s\n' " ${vresult} "  >> "${ERROR_EXIT_FILE}"
    fi

    if [[ $exitcode -gt 0 ]]
    then

        echo -e "\n exitcode: " $exitcode: $(basename $filename)" \n"  >> "${ERROR_EXIT_FILE}"
    fi

