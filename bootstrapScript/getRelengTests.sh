#!/usr/bin/env bash

# it is assumed we are executing this in RELENG_TESTS or the parent of RELENG_TESTS

# Note: for deployment on production machine, no "custom" properties need to be set in aggr_properties.shsource, 
# assuming the "hudson build script" has been set up appropriately. 
# 1. need to set "use custom workspace" so some of the relative directory assumptions are true ... 
     such as for Luna, set 
#    /shared/simrel/${release}/org.eclipse.simrel.tests
# 2. copy "by hand" (or scp) this getRelengTests.sh file to /shared/simrel/${release} and run from hudson from there, the parent of 
#    o.e.i.tests, namely "run shell script" /shared/simrel/${release}/getRelengTests.sh
# 3. We currently assume "testInstance" already exists, as a child of /shared/simrel/${release}, and contains an instance of eclipse SDK (3.7). 


# finds file on users path, before current directory
# hence, non-production users can set their own values for test machines
# not expected on production machine, so we send error output to bit bucket
source aggr_properties.shsource 2>/dev/null

RELENG_TESTS=${RELENG_TESTS:-org.eclipse.simrel.tests}
BRANCH_TESTS=${BRANCH_TESTS:-master}
TMPDIR_TESTS=${TMPDIR_TESTS:-sbtests}
CGITURL=${CGITURL:-http://git.eclipse.org/c/simrel}

function usage() 
{
    printf "\n\tUsage: %s [-f] [-v] " $(basename $0) >&2
    printf "\n\t\t%s\t%s" "-f" "Allow fresh creation (confirm correct current directory)." >&2
    printf "\n\t\t%s\t%s" "-c" "Force clean of testInstance directory" >&2
    printf "\n\t\t%s\t%s\n" "-v" "Print verbose debug info." >&2
}


verboseFlag=false
freshFlag=false
cleanFlag=false
while getopts 'hvfc' OPTION
do
    case $OPTION in
        h)    usage
        exit 1
        ;;
        v)    verboseFlag=true
        ;;
        f)    freshFlag=true
        ;;
        c)    cleanFlag=true
        ;;
        ?)    usage
        exit 2
        ;;
    esac
done

# This shift is not required in our particular, current case, 
# But is a common pattern to leave command line args at correct 
# point, so we leave it in. 
shift $(($OPTIND - 1))

# 'env' is handy to print all env variables to log, 
# if needed for debugging
if $verboseFlag
then
    env
    echo "fresh install: $freshFlag"
    echo "verbose output: $verboseFlag"
    echo "force clean prereqs: $cleanFlag"
    echo "BUILD_TESTS: ${RELENG_TESTS}"
    echo "TMPDIR_TESTS=${TMPDIR_TESTS}"

fi

echo "CGITURL: ${CGITURL}"
echo "BRANCH_TOOLS: ${BRANCH_TESTS}"

# echo current directory
echo "Current Directory: ${PWD}"

# This script file is to help get builds started "fresh", when
# the ${RELENG_TESTS} directory already exists on local file system.
# While it is in the cvs repository in ${RELENG_TESTS}, it is
# meant to be executed from the parent directory
# of ${RELENG_TESTS} on the file system.

# export is used, instead of checkout, just to avoid the CVS directories and since this code
# for a local build, there should never be a need to check it back in to CVS.

# if freshFlag is set, then "not freshFlag" is false and will skip 
# the sanity check.   
if ! $freshFlag && [[ ! -e ${RELENG_TESTS} ]]
then
    echo "${RELENG_TESTS} does not exist as sub directory";
    usage
    exit 1;
fi


# make sure RELENG_TESTS has been defined and is no zero length, or 
# else following will eval to "rm -fr /*" ... potentially catastrophic
if [ -z "${RELENG_TESTS}" ]
then
    echo "The variable RELENG_TESTS must be defined to run this script"
    usage
    exit 1;
fi


echo "    removing all of ${RELENG_TESTS} ..."
rm -fr "${RELENG_TESTS}"
mkdir -p "${RELENG_TESTS}"



# remove if already exists
rm ${BRANCH_TESTS////_}.zip* 2>/dev/null
rm -fr ${TMPDIR_TESTS} 2>/dev/null 

wget --no-verbose -O ${BRANCH_TESTS////_}.zip ${CGITURL}/${RELENG_TESTS}.git/snapshot/${BRANCH_TESTS}.zip 2>&1
RC=$?
if [[ $RC != 0 ]] 
then
    echo "   ERROR: Failed to get ${BRANCH_TESTS}.zip from  ${CGITURL}/${BUILD_TESTS}/snapshot/${BRANCH_TESTS}.zip"
    echo "   RC: $RC"
    usage
    exit $RC
fi

quietZipFlag=-q
if $verboseFlag
then
    quietZipFlag=
fi

unzip ${quietZipFlag} -o ${BRANCH_TESTS////_}.zip -d ${TMPDIR_TESTS} 
RC=$?
if [[ $RC != 0 ]] 
then
    echo "/n/t%s/t%s/n" "ERROR:" "Failed to unzip ${BRANCH_TESTS}.zip to ${TMPDIR_TESTS}"
    echo "   RC: $RC"
    usage
    exit $RC
fi

rsynchvFlag=
if $verboseFlag
then
    rsynchvFlag=-v
fi

rsync $rsynchvFlag -r ${TMPDIR_TESTS}/${BRANCH_TESTS}/ ${RELENG_TESTS}
RC=$?
if [[ $RC != 0 ]] 
then
    echo "ERROR: Failed to copy ${RELENG_TESTS} from ${TMPDIR_TESTS}/${BRANCH_TESTS}/"
    echo "   RC: $RC"
    usage
    exit $RC
fi


echo "    making sure releng control files are executable and have proper EOL ..."
dos2unix ${RELENG_TESTS}/*.sh* ${RELENG_TESTS}/*.properties ${RELENG_TESTS}/*.xml >/dev/null 2>>/dev/null
chmod +x ${RELENG_TESTS}/*.sh > /dev/null
echo "    Done. "

if $cleanFlag
then
    # should very rarely need to do this, Like, once release. 
    # But Eclipse (OSGi?) creates some files with 
    # only group read access, so to complete remove them, must use 
    # hudsonbuild ID to get completely clean. 
    echo "    removing all of testInstance directory"
    rm -fr testInstance
fi

if ! $verboseFlag
then
    # cleanup unless verbose/debugging
    rm ${BRANCH_TESTS////_}.zip* 2>/dev/null
    rm -fr ${TMPDIR_TESTS} 2>/dev/null
fi

# TODO ... a bit quirky ... need to install releng tests using this file, but then 
# also run "installTests" target from releng tools build.xml file. 

exit 0
