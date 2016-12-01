#!/usr/bin/env bash

function writeArtifactsHeader
{
    outfile=$1
    printf "%s\n" "<?xml version='1.0' encoding='UTF-8'?>" > ${outfile}
    printf "%s\n" "<?compositeArtifactRepository version='1.0.0'?>" >> ${outfile}
    printf "%s\n" "<repository name='Eclipse CBI p2 Repository Analyzers'  type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>" >> ${outfile}
    printf "%s\n" "  <properties size='3'>" >> ${outfile}
    printf "%s\n" "    <property name='p2.timestamp' value='1313779613118'/>" >> ${outfile}
    printf "%s\n" "    <property name='p2.compressed' value='true'/>" >> ${outfile}
    printf "%s\n" "    <property name='p2.atomic.composite.loading' value='true'/>" >> ${outfile}
    printf "%s\n" "  </properties>" >> ${outfile}
    printf "%s\n" "  <children size='3'>" >> ${outfile}

}

function writeContentHeader
{
    outfile=$1
    printf "%s\n" "<?xml version='1.0' encoding='UTF-8'?>" > ${outfile}
    printf "%s\n" "<?compositeMetadataRepository version='1.0.0'?>" >> ${outfile}
    printf "%s\n" "<repository name='Eclipse CBI p2 Repository Analyzers'  type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>" >> ${outfile}
    printf "%s\n" "  <properties size='3'>" >> ${outfile}
    printf "%s\n" "    <property name='p2.timestamp' value='1313779613118'/>" >> ${outfile}
    printf "%s\n" "    <property name='p2.compressed' value='true'/>" >> ${outfile}
    printf "%s\n" "    <property name='p2.atomic.composite.loading' value='true'/>" >> ${outfile}
    printf "%s\n" "  </properties>" >> ${outfile}
    printf "%s\n" "  <children size='3'>" >> ${outfile}

}

function writeFooter
{
    outfile=$1
    printf "%s\n" "  </children>" >> ${outfile}
    printf "%s\n" "</repository>" >> ${outfile}
}

function writeCompositeP2Index
{
    printf "%s\n" "version=1" > "${p2Index}"
    printf "%s\n" "metadata.repository.factory.order=compositeContent.xml" >> "${p2Index}"
    printf "%s\n" "artifact.repository.factory.order=compositeArtifacts.xml" >> "${p2Index}"
}

function writeChildren
{
    outfile=$1
    repoRoot=$2
    # NOTE: we always take "most recent 3 builds". 
    # we use "I20" as prefix that all our child repo directories start with 
    # such as "I2016...". So, in 80 years will need some maintenance. :) 
    # But, otherwise, this cheap heuristic finds existing files such as "composite*".
    pushd "${repoRoot}" >/dev/null
    children=$(ls -1td I20* | head -3)
    popd >/dev/null

    for child in $children
    do
        printf "%s%s%s\n" "    <child location='" $child "' />"  >> ${outfile}
    done

}

repoRoots=("/home/data/httpd/download.eclipse.org/cbi/updates/analyzers/4.6")
# Normally "writeRepoRoots" is the same as "repoRoots", but might not always be, plus
# it is very handy for testing this script not to have to write to the "production" area.
#writeRepoRoots=("${PWD}/ide" "${PWD}/headless")
writeRepoRoots=(${repoRoots[@]})
indices=(0)
for index in ${indices[@]} 
do
    #echo -e "[DEBUG] index: ${index}\n"
    writeRepoRoot="${writeRepoRoots[$index]}"
    #echo -e "[DEBUG] writeRepoRoot: ${writeRepoRoot}\n"
    mkdir -p "${writeRepoRoot}"
    RC=$?
    if [[ $RC != 0 ]]
    then
       echo -e "[ERROR] Could not create directory at ${writeRepoRoot}\n"
       exit $RC
    fi
    repoRoot="${repoRoots[$index]}"
    #echo -e "[DEBUG] repoRoot: ${repoRoot}\n"
    
    artifactsCompositeFile="${writeRepoRoot}/compositeArtifacts.xml"
    contentCompositeFile="${writeRepoRoot}/compositeContent.xml"
    p2Index="${writeRepoRoot}/p2.index"

    writeArtifactsHeader "${artifactsCompositeFile}"
    writeChildren "${artifactsCompositeFile}" "${repoRoot}"
    writeFooter "${artifactsCompositeFile}"

    writeContentHeader "${contentCompositeFile}"
    writeChildren "${contentCompositeFile}" "${repoRoot}"
    writeFooter "${contentCompositeFile}"

    writeCompositeP2Index "${p2Index}"
done

