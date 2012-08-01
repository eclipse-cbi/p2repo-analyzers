#!/usr/bin/env bash

# small utility to remove old (temporary) builds from work area

# finds file on users path, before current directory
# hence, non-production users can set their own values for test machines
source aggr_properties.shsource

# remove artifacts over n days old
# (where hours = 24 + (n * 24), basically, so that
# even n=0 means "1 day")

# set at 10 days, under assumption that before that time,
# artifacts will be "saved" elsewhere, if needed.
# if more cleanup is needed, we should take time, and
# existence of more recent builds into account, so we never
# delete the last existing build (even if "old").

ndays=1;
artifactsDir=${BUILD_RESULTS};

echo;
echo "    Removing artifact directories older than ${ndays} days";
echo "        (from ${artifactsDir})";
before=`find ${artifactsDir} -mindepth 1 -maxdepth 1 | wc -l`;
echo "            number of directories before cleaning: ${before}";

# empty directories often result from "bad builds". We remove those no matter how old
find ${artifactsDir} -mindepth 1 -maxdepth 2 -type d -empty -exec rm -fr '{}' \;
# now remove old ones
find ${artifactsDir} -mindepth 1 -maxdepth 1 -type d -ctime +$ndays -execdir ${BUILD_HOME}${BUILD_TOOLS}/removeIf.sh '{}' \;

after=`find ${artifactsDir} -mindepth 1 -maxdepth 1 | wc -l`;
echo "            number of directories after cleaning: ${after}";
echo
