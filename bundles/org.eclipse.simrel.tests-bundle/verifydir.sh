#!/usr/bin/env bash

# we make the assumption that 'verify.sh' is in same directory as 'verifydir.sh'
# so the correct 'verify.sh' file command can be used; the one that "goes with" the verifydir.sh script
# we are currently executing. So, we store the path to verifydir.sh in EXECDIR.
EXECDIR=${0%/*}

echo "1: " $1
echo "2: " $2
echo "3: " $3

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

# make, in case doesn't exist
mkdir -p "${VERIFYOUTDIR}"

# even though we just set it above, include the if check
# here to gaurd against future errors, since if 'verfiyoutdir' is
# not defined, the remove would be for the root directory!!
if [[ -n "${VERIFYOUTDIR}" ]]
then
  rm -fr "${VERIFYOUTDIR}"/*
fi

echo "   verify directory: ${loc}"
echo "   for file pattern: ${pat}"
echo "   result list files will be in " "${VERIFYOUTDIR}" "(and any existing files there removed)."

find "${loc}" -regex "${pat}" -exec ${EXECDIR}/verify.sh '{}' \;

# if certain files were not created during signing checks, create a "plain" one at expected location,
# to avoid 404 errors from standard index.html. For example, all jars are signed, there will be no unsigned.txt files.
if [ ! -e "${VERIFYOUTDIR}"/unsigned.txt ]
then
  echo  "There were no unsigned jars in the directories checked. " > "${VERIFYOUTDIR}"/unsigned.txt
fi
if [ ! -e "${VERIFYOUTDIR}"/nestedjars.txt ]
then
  echo  "There were no nested packed jars in the directories checked. " > "${VERIFYOUTDIR}"/nestedjars.txt
fi
