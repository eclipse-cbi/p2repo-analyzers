#!/usr/bin/env bash

if [[ -z "${release}" ]]
then
    echo 
    echo "   ERRRO: The 'release' environment much be specified for this script. For example,"
    echo "   release=kepler ./$( basename $0 )"
    echo
    exit 1
else
    echo
    echo "release: ${release}"
    echo
fi
 
source aggr_properties.shsource
source ${PWD}/aggr_properties.shsource

# These first few variables commonly need to overridden on test machines, they 
# are very specific per machine. 
# the top directory of build-related directories
export BUILD_HOME=${BUILD_HOME:-/shared/simrel/${release}}
export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}

# These remaining variable should not need to be overridden, as they 
# are relatively constant, or computed from others. 
export RELENG_TESTS_DIR=${RELENG_TESTS_DIR:-org.eclipse.simrel.tests}
export JAVA_EXEC_DIR=${JAVA_EXEC_DIR:-${JAVA_HOME}/jre/bin}
export BUILD_TESTS_DIR=${BUILD_TESTS_DIR:-${BUILD_HOME}/${RELENG_TESTS_DIR}}
export ECLIPSE_HOME_TEST=${ECLIPSE_HOME_TEST:-${BUILD_HOME}/testInstance}
export export ECLIPSE_TEST_EXE=${ECLIPSE_TEST_EXE:-${ECLIPSE_HOME_TEST}/eclipse/eclipse}

"${ECLIPSE_TEST_EXE}" -consolelog -data ${BUILD_TESTS_DIR}/eclipseWorkspace -nosplash --launcher.suppressErrors -vm "${JAVA_EXEC_DIR}" -application org.eclipse.ant.core.antRunner -f  ${BUILD_TESTS_DIR}/runTests.xml "$@"
