#!/bin/sh

source aggr_properties.shsource

ANT_CMD="${ANT_HOME}/bin/ant"
${ANT_HOME}/bin/ant "$@"
