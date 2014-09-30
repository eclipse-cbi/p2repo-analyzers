#!/bin/sh

source aggr_properties.shsource

if [ -z $ANT_HOME ] ; then
   echo "ANT_HOME must be defined to use this script.";
   exit 1;
fi

echo "Starting ant via shell script"
echo "ANT_HOME: " ${ANT_HOME}

ANT_CMD="${ANT_HOME}/bin/ant"
${ANT_HOME}/bin/ant "$@" &
