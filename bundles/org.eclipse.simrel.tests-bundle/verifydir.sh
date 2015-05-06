#!/usr/bin/env bash

# we make the assumption that 'verify.sh' is in same directory as 'verifydir.sh' 
# so the correct 'verify.sh' file command can be used; the one that "goes with" the verifydir.sh script 
# we are currently executing. So, we store the path to verifydir.sh in EXECDIR.  
EXECDIR=${0%/*}

function info () 
{
    echo -e "\n\tverify directory: ${loc}"
    echo -e "\n\tfor file pattern: ${pat}"
    echo -e "\n\tresult list files are in \n${VERIFYOUTDIR}\n"
}

function pause () 
{
    TIMEOUT=$1
    if [[ -z ${TIMEOUT} ]]  
    then
        TIMEOUT=10
    fi
    echo -e "\n"
    read -t$TIMEOUT -n1 -r -p "Result list files will be in ${VERIFYOUTDIR} (and any existing files there removed) unless ctrl-c pressed before 5 seconds ..." key
    echo -e "\n"
    if [ $? -eq 0 ]; then
        return 0; #echo A key was pressedi: $key.
    else
        return 1; #echo No key was pressed.
    fi
}



echo -e "\ncommand line location to scan:  \t$1"
echo -e "\ncommand line verifydiroutput parent:  \t$2"
echo -e "\ncommand line file pattern to match:  \t$3\n"

if [ -z $1 ]
then
    loc=./
else
    loc=${1}
fi

if [ -z $2 ]
then
    export VERIFYOUTDIR="${HOME}"/verifydiroutput
else
    export VERIFYOUTDIR="${2}"/verifydiroutput
fi

if [ -z $3 ]
then
    pat=".*\(\.jar$\|\.jar\.pack\.gz$\)"
else
    pat="${3}"
fi

# VERIFYOUTDIR is used in 'verify.sh' and must be clean before
# beginning the overall check
if [[ -e "${VERIFYOUTDIR}" ]]
then
    pause
fi

# make, in case doesn't exist
mkdir -p "${VERIFYOUTDIR}"

# even though we just created it above, include the following if check 
# here to gaurd against future changes, since if 'verfiyoutdir' is 
# not defined, the remove would be for the root directory!! 
if [[ -n "${VERIFYOUTDIR}" ]]
then 
    rm -fr "${VERIFYOUTDIR}"/*
fi

# remember, best to pass in env variable of ${WORKSPACE}/tmp when running on Hudson
export TMP_DIR=${TMP_DIR:-/tmp}

# We display general info at beginning, and at end, just so it is easier not to miss.
info

# Adjust the "list of versions" to check more than one Java version
#for ver in 8 7 6 
for ver in 8
do
find "${loc}" -regex "${pat}" -exec ${EXECDIR}/verify.sh '{}' $ver \;
done

info

# if certain files were not created during signing checks, create a "plain" one at expected location, 
# to avoid 404 errors from standard index.html. For example, all jars are signed, there will be no unsigned.txt files.
# TODO: change HTML to PHP, so we can "check for the existence" of these files ... i.e. easer to flag 
# an error if exists or not, instead of based on size? 
#if [ ! -e "${VERIFYOUTDIR}"/unsigned.txt ]
#then
#    echo  "There were no unsigned jars in the directories checked. " > "${VERIFYOUTDIR}"/unsigned.txt
#fi 
#if [ ! -e "${VERIFYOUTDIR}"/nestedjars.txt ]
#then
#    echo  "There were no nested packed jars in the directories checked. " > "${VERIFYOUTDIR}"/nestedjars.txt
#fi 
