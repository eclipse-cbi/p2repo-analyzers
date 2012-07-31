#!/usr/bin/env bash

if [[ $# != 1 ]] 
then
    printf "\tERROR: this script, "${0##*/}", requires exactly one argument; the name or path of jar or jar.pack.gz file.\n" >&2
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

jarname=$(basename "${filename}")

# most users need only one, Java 5 or Java 6, but sometimes may want to 
# change JAVA_HOME, below, to switch back and forth.
# Use JAVA 5 to ensure pack.gz files can be unpacked (installed) with JAVA 5. 
# Some pack.gz files can not be unpacked with Java 6 (in SimRel repositories), 
# so use JAVA 6 if you do not care (i.e. your use cases all use JAVA 6)
# to avoid the error messages.
#JAVA_5_HOME=/home/shared/webtools/apps/ibm-java2-i386-5011
#JAVA_5_HOME=/shared/common/jdk-1.5.0-22.x86_64/
#JAVA_6_HOME=/home/shared/webtools/apps/ibm-java-i386-608
#JAVA_6_HOME=/shared/common/jdk-1.6.x86_64/

# We always set JAVA_HOME explicitly to what we want, since on many systems, 
# is it set to some JRE that would not suffice. 
#JAVA_HOME=${JAVA_6_HOME}

# TODO: this file may be written hundreds of times, but important to have record, 
# to we'll improve logging later.
echo "JAVA_HOME: ${JAVA_HOME}" > "${VERIFYOUTDIR}"/info.txt
echo "verify script: ${0}" >> "${VERIFYOUTDIR}"/info.txt


if [ ! -e "${JAVA_HOME}" ]
then
    printf "\tERROR: this script, "${0##*/}", requires JAVA_HOME to be defined and exist.\n" >&2
    exit 1
fi

UNPACK200_EXE=$JAVA_HOME/jre/bin/unpack200
VERIFY_EXE=$JAVA_HOME/bin/jarsigner

    VERIFIED_OUTFILE="${VERIFYOUTDIR}"/verified.txt
    KNOWN_EXCEPTION="${VERIFYOUTDIR}"/knownunsigned.txt
    UNSIGNED_OUTFILE="${VERIFYOUTDIR}"/unsigned.txt
    NOMANIFEST="${VERIFYOUTDIR}"/nomanifest.txt
    ERROR_EXIT_FILE="${VERIFYOUTDIR}"/error.txt

PPAT_PACKGZ="(.*).pack.gz$"
if [[ "$jarname" =~  $PPAT_PACKGZ ]]
then 
    basejarname=${BASH_REMATCH[1]}
    #echo -e "\n basejarname: " $basejarname "\n"
    "${UNPACK200_EXE}" $filename /tmp/$basejarname
    vresult=`"${VERIFY_EXE}" -verify /tmp/$basejarname`
    exitcode=$?
    rm /tmp/$basejarname
else
    vresult=`"${VERIFY_EXE}" -verify $filename`
    exitcode=$?
fi

# jarsigner sometimes returns one line, sometimes two ... we take 
# out EOLs to print compactly
vresult=`echo "${vresult}" | tr '\n' ' '`

# known response patterns from jarsigner
PPAT_VERIFIED="^jar\ verified.*"
# no manifest is not signed for our purposes ... occurs a lot for unsigned feature jars
PPAT_UNSIGNED_OR_NOMANIFEST="^(jar is unsigned)|(no manifest).*"
# do not currently use unsigned or no manifest (by themselves) 
# nor "copy mode" ... copy mode printed to stdout by unpack200
#PPAT_UNSIGNED="^jar is unsigned.*"
#PPAT_NOMANIFEST="^no manifest.*"
#PPAT_COPYMODE="^Copy-mode\..*"

if [[ "${vresult}" =~ $PPAT_VERIFIED ]]
then
    printf '%-80s \t\t' "   ${jarname}: " >> "${VERIFIED_OUTFILE}" 
    printf '%s\n' " ${vresult} " >> "${VERIFIED_OUTFILE}"
elif [[ "${vresult}" =~ $PPAT_UNSIGNED_OR_NOMANIFEST ]]
then

    # list "known cases", that can not be signed, 
    # in their own "known exception file", else list in "unsigned" file.

    # For reasons of addition of org.eclipse.jdt.core.compiler.batch see
    # https://bugs.eclipse.org/bugs/show_bug.cgi?id=356382
    # For small pointer about commonj.sdo, see
    # https://bugs.eclipse.org/bugs/show_bug.cgi?id=276999
    PPAT_KNOWN_UNSIGNED='(org\.eclipse\.jdt\.core\.compiler\.batch.*)|(commonj\.sdo.*)|(artifacts\.jar)|(content\.jar)|(compositeArtifacts.jar)|(compositeContent.jar)'

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

