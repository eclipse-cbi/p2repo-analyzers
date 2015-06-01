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

if [ -z "${1}" ]
then
  loc=${PWD}
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
  rm -fr "${VERIFYOUTDIR}"
fi

# make, in case doesn't exist
mkdir -p "${VERIFYOUTDIR}"

UNPACK200_EXE=$JAVA_HOME/jre/bin/unpack200
VERIFY_EXE=$JAVA_HOME/bin/jarsigner
#VERIFY_OPTIONS=${VERIFY_OPTIONS:-"-verify -verbose  -certs"
#COMPACT=${COMPACT:-false}
#VERIFY_OPTIONS=${VERIFY_OPTIONS:-"-verify -verbose"}
#COMPACT=${COMPACT:-false}
VERIFY_OPTIONS=${VERIFY_OPTIONS:-"-verify"}
COMPACT=${COMPACT:-true}

#TMP_DIR is used by verify.sh when it unpack's things
now=$( date --utc +%s )
export TMP_DIR=${TMP_DIR:-/shared/simrel/tmp/$(date --utc -d @$now +%Y-%m%d_%H%M-%S)}

# We display general info at beginning, and at end, just so it is easier not to miss.
info
# 6 7 
for ver in 8 
do
  # These are needed by verify.sh, but we define them here, so we can sort 
  # them just once, at end.
  CHECK_SUM_FILE="${VERIFYOUTDIR}/checksums${ver}.txt"
  VERIFIED_OUTFILE="${VERIFYOUTDIR}/verified${ver}.txt"
  KNOWN_EXCEPTION="${VERIFYOUTDIR}/knownunsigned${ver}.txt"
  UNSIGNED_OUTFILE="${VERIFYOUTDIR}/unsigned${ver}.txt"
  NOMANIFEST="${VERIFYOUTDIR}/nomanifest${ver}.txt"
  ERROR_EXIT_FILE="${VERIFYOUTDIR}/error${ver}.txt"
  NESTED_JARS="${VERIFYOUTDIR}/nestedjars${ver}.txt"

  find "${loc}" -regex "${pat}" -exec ${EXECDIR}/verify.sh '{}' $ver "${loc}" \;
done

info

if [[ -e  ${VERIFYOUTDIR}/checksums${ver}.txt ]]
then
  sort  -k1.1,90 -k3.1,10 -r < ${VERIFYOUTDIR}/checksums${ver}.txt > ${VERIFYOUTDIR}/checksums${ver}-sorted.txt
fi
# When 'compact' thing sort well, but, not if non-compact output.
if [[ "true" == "${COMPACT}" ]] 
then

  if [[ -e "${VERIFIED_OUTFILE}" ]]
  then
    sort -o "${VERIFIED_OUTFILE}" "${VERIFIED_OUTFILE}"
  fi
  if [[ -e "${UNSIGNED_OUTFILE}" ]]
  then
    sort -o "${UNSIGNED_OUTFILE}" "${UNSIGNED_OUTFILE}"
  fi
  if [[ -e "${KNOWN_EXCEPTION}" ]]
  then
    sort -o "${KNOWN_EXCEPTION}" "${KNOWN_EXCEPTION}"
  fi
  if [[ -e "${NOMANIFEST}" ]]
  then
    sort -o "${NOMANIFEST}" "${NOMANIFEST}"
  fi
fi

# if certain files were not created during signing checks, create a "plain" one at expected location, 
# to avoid 404 errors from standard index.html. For example, all jars are signed, there will be no unsigned.txt files.
# TODO: change HTML to PHP, so we can "check for the existence" of these files ... i.e. easer to flag 
# an error if exists or not, instead of based on size? 
if [ ! -e "${VERIFYOUTDIR}"/unsigned${ver}.txt ]
then
    echo  "There were no unsigned jars in the directories checked. " > "${VERIFYOUTDIR}"/unsigned${ver}.txt
fi 
if [ ! -e "${VERIFYOUTDIR}"/errors${ver}.txt ]
then
    echo  "There were no unsigned jars in the directories checked. " > "${VERIFYOUTDIR}"/unsigned${ver}.txt
fi 
if [ ! -e "${VERIFYOUTDIR}"/nestedjars${ver}.txt ]
then
    echo  "There were no nested packed jars in the directories checked. " > "${VERIFYOUTDIR}"/nestedjars${ver}.txt
fi 
