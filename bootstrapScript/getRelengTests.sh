#!/usr/bin/env bash

# it is assumed we are executing this in RELENG_TESTS or the parent of RELENG_TESTS

# Note: for deployment on production machine, no "custom" properties need to be set in aggr_properties.shsource, 
# assuming the "hudson build script" has been set up appropriately. 
# 1. need to set "use custom workspace" so some of the relative directory assumptions are true ... such as for Juno, set 
#    /shared/juno/org.eclipse.simrel.tests
# 2. copy "by hand" (or scp) this getRelengTests.sh file to /shared/juno and run from hudson from there, the parent of 
#    o.e.i.tests, namely "run shell script" /shared/juno/getRelengTests.sh
# 3. We currently assume "testInstance" already exists, as a child of /shared/juno, and contains an instance of eclipse SDK (3.7). 


# finds file on users path, before current directory
# hence, non-production users can set their own values for test machines
source aggr_properties.shsource

if [ -z ${RELENG_TESTS} ] ; then

    RELENG_TESTS=org.eclipse.simrel.tests
    echo "RELENG_TESTS set from script: " $RELENG_TESTS
else
    echo "RELENG_TESTS set from aggr_properties.shsource: " $RELENG_TESTS

fi

# This script file is to help get builds started "fresh", when
# the ${RELENG_TESTS} directory already exists on local file system.
# While it is in the cvs repository in ${RELENG_TESTS}, it is
# meant to be executed from the parent directory
# of ${RELENG_TESTS} on the file system.

# export is used, instead of checkout, just to avoid the CVS directories and since this code
# for a local build, there should never be a need to check it back in to CVS.

# If there is no subdirectory, try going up one directory and looking again (in case we are in it).
if [ ! -e ${RELENG_TESTS} ]
then
    cd ..
    if [ ! -e ${RELENG_TESTS} ]
    then        
        echo "${RELENG_TESTS} does not exist as sub directory";
        exit 1;
    fi
fi


# make sure RELENG_TESTS has been defined and is no zero length, or 
# else following will eval to "rm -fr /*" ... potentially catastrophic
if [ -z "${RELENG_TESTS}" ]
then
    echo "The variable RELENG_TESTS must be defined to run this script"
    exit 1;
fi
echo "    removing all of ${RELENG_TESTS} ..."
rm -fr "${RELENG_TESTS}"/*
rm -fr "${RELENG_TESTS}"/.project
rm -fr "${RELENG_TESTS}"/.settings
rm -fr "${RELENG_TESTS}"/.classpath
mkdir -p "${RELENG_TESTS}"

BRANCH_TESTS=${BRANCH_TESTS:-master}
TMPDIR_TESTS=${TMPDIR_TESTS:-sbtests}
CGITURL=${CGITURL:-http://git.eclipse.org/c/simrel/}


echo "PWD: ${PWD}"
rm ${BRANCH_TESTS}.zip*

echo "PWD: ${PWD}"
wget  ${CGITURL}/${RELENG_TESTS}/snapshot/${BRANCH_TESTS}.zip 2>&1
RC=$?
if [[ $RC != 0 ]] 
then
    echo "   ERROR: Failed to get ${BRANCH_TESTS}.zip from  ${CGITURL}/${BUILD_TESTS}/snapshot/${BRANCH_TESTS}.zip"
    echo "   RC: $RC"
    exit $RC
fi

echo "PWD: ${PWD}"
unzip -o ${BRANCH_TESTS}.zip -d ${TMPDIR_TESTS} 
RC=$?
if [[ $RC != 0 ]] 
then
    printf "/n/t%s/t%s/n" "ERROR:" "Failed to unzip ${BRANCH_TESTS}.zip to ${TMPDIR_TESTS}"
        echo "   RC: $RC"
    exit $RC
fi

rsync -vr ${TMPDIR_TESTS}/${BRANCH_TESTS}/ ${RELENG_TESTS}
RC=$?
if [[ $RC != 0 ]] 
then
    printf "/n/t%s/t%s/n" "ERROR:" "Failed to copy ${RELENG_TESTS} from ${TMPDIR_TESTS}/${BRANCH_TESTS}/"
        echo "   RC: $RC"
        exit $RC
fi


echo "    making sure releng control files are executable and have proper EOL ..."
dos2unix ${RELENG_TESTS}/*.sh* ${RELENG_TESTS}/*.properties ${RELENG_TESTS}/*.xml >/dev/null 2>>/dev/null
chmod +x ${RELENG_TESTS}/*.sh > /dev/null
echo

# TODO ... a bit quirky ... need to install releng tests as above, but then 
# also run "installTests" target from releng tools build.xml file. 


