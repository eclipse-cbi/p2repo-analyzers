#!/usr/bin/env bash



# Note: for deployment on production machine, no "custom" properties need to be set in aggr_properties.shsource, 
# assuming the "hudson build script" has been set up appropriately. 
# 1. need to set "use custom workspace" so some of the relative directory assumptions are true ... 
#    such as for Luna, set 
#    /shared/simrel/${release}/org.eclipse.simrel.tests
# 2. copy "by hand" (or scp) this getRelengTests.sh file to /shared/simrel/${release} and run from hudson from there, the parent of 
#    o.e.i.tests, namely "run shell script" /shared/simrel/${release}/getRelengTests.sh
# 3. We currently assume "testInstance" already exists, as a child of /shared/simrel/${release}, and (currently) contains an instance of eclipse SDK (4.3). 

# if freshFlag is set, then "not freshFlag" is false and will skip 
# the sanity check.   
if ! $freshFlag && [[ ! -e ${BUILD_TOOLS} ]]
then
    echo "${BUILD_TOOLS} does not exist as sub directory";
    usage
    exit 1;
fi


# remember to leave no slashes on first filename in source command,
# so that users path is used to find it (first, if it exists)
# variables that user might want/need to override, should be defined, 
# in our own aggr_properties.shsource using the X=${X:-"xyz"} syntax.
source aggr_properties.shsource 2>/dev/null
source ${BUILD_HOME}/org.eclipse.simrel.tests/aggr_properties.shsource

RELENG_TESTS_REPO=${RELENG_TESTS_REPO:-org.eclipse.simrel.tests}
RELENG_TESTS_PATH=${RELENG_TESTS_PATH:-bundles/org.eclipse.simrel.tests-bundle}
RELENG_TESTS_DIR=${RELENG_TESTS_DIR:-org.eclipse.simrel.tests}
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
    echo -e "\n\t == Verbose mode. =="
    env > envSettings.txt
    echo -e "\tSee 'envSettings.txt' for environment settings."
    echo -e "\tfresh install: $freshFlag"
    echo -e "\tverbose output: $verboseFlag"
    echo -e "\tforce clean prereqs: $cleanFlag"
    echo -e "\tRELENG_TESTS_REPO: ${RELENG_TESTS_REPO}"
    echo -e "\tRELENG_TESTS_PATH: ${RELENG_TESTS_PATH}"
    echo -e "\tRELENG_TESTS_DIR: ${RELENG_TESTS_DIR}"
    echo -e "\tTMPDIR_TESTS=${TMPDIR_TESTS}"
    echo -e "\t == == \n"
else
    echo -e "\t == Non-verbose mode. == "
fi

echo "CGITURL: ${CGITURL}"
echo "BRANCH_TESTS: ${BRANCH_TESTS}"

# echo current directory
echo "Current Directory: ${PWD}"

# This script file is to help get builds started "fresh", when
# the ${RELENG_TESTS_REPO} directory already exists on local file system.
# While it is in the cvs repository in ${RELENG_TESTS_REPO}, it is
# meant to be executed from the parent directory
# of ${RELENG_TESTS_REPO} on the file system.

# export is used, instead of checkout, just to avoid the CVS directories and since this code
# for a local build, there should never be a need to check it back in to CVS.

# if freshFlag is set, then "not freshFlag" is false and will skip 
# the sanity check.   
if ! $freshFlag && [[ ! -e ${RELENG_TESTS_DIR} ]]
then
    echo "${RELENG_TESTS_DIR} does not exist as sub directory";
    usage
    exit 1;
fi


# make sure RELENG_TESTS has been defined and is no zero length, or 
# else following will eval to "rm -fr /*" ... potentially catastrophic
if [ -z "${RELENG_TESTS_DIR}" ]
then
    echo "The variable RELENG_TESTS_DIR must be defined to run this script"
    usage
    exit 1;
fi


echo "    removing all of ${RELENG_TESTS_DIR} ..."
rm -fr "${RELENG_TESTS_DIR}"
mkdir -p "${RELENG_TESTS_DIR}"

# remove if already exists from previous run
rm ${RELENG_TESTS_REPO////_}.zip* 2>/dev/null
rm -fr ${TMPDIR_TESTS} 2>/dev/null 

wget --no-verbose -O ${RELENG_TESTS_REPO////_}.zip ${CGITURL}/${RELENG_TESTS_REPO}.git/snapshot/${BRANCH_TESTS}.zip 2>&1
RC=$?
if [[ $RC != 0 ]] 
then
    echo "   ERROR: Failed to get ${RELENG_TESTS_REPO////_}.zip from  ${CGITURL}/${RELENG_TESTS_REPO}/snapshot/${BRANCH_TESTS}.zip"
    echo "   RC: $RC"
    usage
    exit $RC
fi

quietZipFlag=-q
if $verboseFlag
then
    quietZipFlag=
fi

unzip ${quietZipFlag} -o ${RELENG_TESTS_REPO////_}.zip -d ${TMPDIR_TESTS} 
RC=$?
if [[ $RC != 0 ]] 
then
    echo "/n/t%s/t%s/n" "ERROR:" "Failed to unzip ${RELENG_TESTS_REPO////_}.zip to ${TMPDIR_TESTS}"
    echo "   RC: $RC"
    usage
    exit $RC
fi

rsynchvFlag=
if $verboseFlag
then
    rsynchvFlag=-v
fi

rsync $rsynchvFlag -r ${TMPDIR_TESTS}/${BRANCH_TESTS}/${RELENG_TESTS_PATH}/ ${RELENG_TESTS_DIR}/
RC=$?
if [[ $RC != 0 ]] 
then
    echo "ERROR: Failed to copy ${RELENG_TESTS_DIR} from ${TMPDIR_TESTS}/${BRANCH_TESTS}/${RELENG_TESTS_PATH}/"
    echo "   RC: $RC"
    usage
    exit $RC
fi


echo "    making sure releng control files are executable and have proper EOL ..."
dos2unix ${RELENG_TESTS_DIR}/*.sh* ${RELENG_TESTS_DIR}/*.properties ${RELENG_TESTS_DIR}/*.xml >/dev/null 2>>/dev/null
chmod +x ${RELENG_TESTS_DIR}/*.sh > /dev/null
echo "    Done. "

if $cleanFlag
then
    # should very rarely need to do this, Like, once release. 
    # But Eclipse (OSGi?) creates some files with 
    # only group read access, so to completly remove them, must use 
    # hudsonbuild ID to get completely clean. 
    echo "    removing all of testInstance directory"
    rm -fr testInstance
fi

#if ! $verboseFlag
#then
## cleanup unless verbose/debugging
rm ${RELENG_TESTS_REPO////_}.zip* 
rm -fr ${TMPDIR_TESTS} 
#fi

# TODO ... a bit quirky ... need to install releng tests using this file, but then 
# also run "installTests" target from releng tools build.xml file. 

exit 0
