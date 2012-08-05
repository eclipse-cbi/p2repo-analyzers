#!/usr/bin/env bash

# On production machine, it is normal to "not find" this file when running. 
# All variables that might be defined in it have correct values set for the production 
# machine. Its purpose is if/when running on a test machine, the variables can be over ridden
# easily in that one file, and leave original scripts alone. 
source aggr_properties.shsource

# These first few variables commonly need to overridden on test machines, they 
# are very specific per machine. 
# the top directory of build-related directories
export BUILD_HOME=${BUILD_HOME:-/shared/juno/${release}}
export JAVA_HOME=${JAVA_HOME:-/shared/orbit/apps/ibm-java-i386-60}

# These remaining variable should not need to be overriddent, as they 
# are relatively constant, or computed from others. 
export RELENG_TESTS=${RELENG_TESTS:-org.eclipse.simrel.tests}
export JAVA_EXEC_DIR=${JAVA_EXEC_DIR:-${JAVA_HOME}/jre/bin}
export BUILD_TESTS_DIR=${BUILD_TESTS_DIR:-${BUILD_HOME}/${RELENG_TESTS}}
export ECLIPSE_HOME_TEST=${ECLIPSE_HOME_TEST:-${BUILD_HOME}/testInstance}
export ECLIPSE_TEST_EXE=${ECLIPSE_TEST_EXE:-${ECLIPSE_HOME_TEST}/eclipse/eclipse}

"${ECLIPSE_TEST_EXE}" -consolelog -data ./eclipseWorkspace -nosplash --launcher.suppressErrors -vm "${JAVA_EXEC_DIR}" -application org.eclipse.ant.core.antRunner -f  ${BUILD_TESTS_DIR}/runTests.xml "$@"
