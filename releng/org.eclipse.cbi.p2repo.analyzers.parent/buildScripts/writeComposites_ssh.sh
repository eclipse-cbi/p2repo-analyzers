#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2020 Eclipse Foundation and others.
# This program and the accompanying materials are made available
# under the terms of the Eclipse Public License 2.0
# which is available at http://www.eclipse.org/legal/epl-v20.html
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************

# This script is similar to /org.eclipse.cbi.p2repo.releng.parent/buildScripts/writeComposites.sh

# Bash strict-mode
set -o errexit
set -o nounset
set -o pipefail

script_name="$(basename ${0})"

username="genie.cbi"
host="projects-storage.eclipse.org"
REPO_BASE_DIR="/home/data/httpd/download.eclipse.org/cbi/updates/analyzers"
UPDATE_RELEASE="${1:-}"

usage() {
  printf "Usage: %s update_release\n" "$script_name"
  printf "\t%-16s Update release (e.g. 4.7).\n" "update_release"
}

# check that update_release is not empty
if [[ -z "${UPDATE_RELEASE}" ]]; then
  printf "ERROR: update release must be given.\n"
  usage
  exit 1
fi

write_header() {
  local outfile=$1
  local type=$2
  cat > "${outfile}" <<EOL
<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='Eclipse CBI p2 Repository Analyzers'  type='org.eclipse.equinox.internal.p2.metadata.repository.${type}' version='1.0.0'>
  <properties size='3'>
    <property name='p2.timestamp' value='1313779613118'/>
    <property name='p2.compressed' value='true'/>
    <property name='p2.atomic.composite.loading' value='true'/>
  </properties>
EOL
}

write_footer() {
  local outfile=$1
  cat >> "${outfile}" <<EOL
  </children>
</repository>
EOL
}

write_composite_P2Index() {
  local outfile=$1
  cat > "${outfile}" <<EOL
version=1
metadata.repository.factory.order=compositeContent.xml
artifact.repository.factory.order=compositeArtifacts.xml
EOL
}

write_composite_repo () {
  local outfile=$1
  local repoDir=$2
  local type=$3

  write_header "${outfile}" "${type}"

  # NOTE: we always take "most recent 3 builds".
  # we use "I20" as prefix that all our child repo directories start with
  childrenRaw=$(ssh ${username}@${host} ls -1td ${repoDir}/I20*)
  childrenBasename=$(echo -e "${childrenRaw}" | xargs -n1 basename)
  #echo -e "${childrenBasename}"
  childrenHead3=$(echo -e "${childrenBasename}" | head -3)
  nChildren=$(echo -e "${childrenHead3}" | wc -l)
  echo "  <children size='${nChildren}'>" >> "${outfile}"
  # DO NOT PUT ${childrenHead3} IN QUOTES HERE !
  for child in ${childrenHead3}
  do
    echo "    <child location=\"${child}\" />" >> "${outfile}"
  done

  write_footer "${outfile}"
}

create_composite_repo() {
  local repoDir=$1
  mkdir -p "${repoDir}"

  write_composite_repo "${repoDir}/compositeArtifacts.xml" "${REPO_BASE_DIR}/${repoDir}" "CompositeArtifactRepository"
  write_composite_repo "${repoDir}/compositeContent.xml" "${REPO_BASE_DIR}/${repoDir}" "CompositeMetadataRepository"
  write_composite_P2Index "${repoDir}/p2.index"

  echo "[DEBUG]:";
  ls -al "${repoDir}"
  cat "${repoDir}/compositeArtifacts.xml"
  cat "${repoDir}/compositeContent.xml"

  scp "${PWD}/${repoDir}"/* "${username}@${host}:${REPO_BASE_DIR}/${repoDir}/"
}

create_composite_repo "${UPDATE_RELEASE}"
