#!/usr/bin/env bash

# This job is ran after a clean build is ran in the "cleanAndDeploy" Hudson job.

# The default for build home is simply where I do my local build. Feel free to change in your local copy. 
build_home=${WORKSPACE:-/home/davidw/gitCBI}



# = = = = = = Build is over, now for some follow-up work. = = = = =
# In this section we convert the mavenproperties.properties into a format 
# that can be read by bash an by PHP. 
propertiesfile="${build_home}/org.eclipse.cbi.p2repo.aggregator/org.eclipse.cbi.p2repo.cli.product/target/mavenproperties.properties"
sourceProperties="${build_home}/mavenproperties.shsource"
phpProperties="${build_home}/mavenproperties.php"

function convertProperties
{
  file=$1
  outfileSource=$2
  outfilephp=$3
  if [ -f "$file" ]
  then
    #echo -e "\n[DEBUG] properties found at expected location: \n\t$file\n"
    echo "# Ant properties translated to bash shell variables" > $outfileSource
    # Note: we intentionally leave php file "unclosed" since it is included
    # in another php file, we assume.
    echo -e "<?php\n// Ant properties translated to PHP variables" > $outfilephp

    while IFS='=' read -r key value
    do
      # Technically, we only need 'buildId' and 'updateRelease' for now (no periods in var name) so 
      # we do not need the space and period translations. But, would in more
      # complicated cases.
      if [[ -n $key ]] 
      then
        # first handle comments. Lines that start with an "ant comment character" ('!' or '#') are 
        # written verbatim, but with approperite comment character ('#' for bash and '//' for php).
        if [[ $key =~ ^!(.*)$ || $key =~ ^#(.*)$ ]]
        then
          echo "#"${BASH_REMATCH[1]} >> $outfileSource
          echo "//"${BASH_REMATCH[1]} >> $outfilephp
        else
          # We only write variables if value is defined. (Otherwise, 
          # can easily provide "illegal" values.) We may want to provide some
          # default value such as "NOT_DEFINED" in come use-cases, but will 
          # wait until we have such a use-case.
          if [[ -n "${value}" && ! "${value}" =~ ^\$\{.*\}$ ]]
          then
            key=$(echo $key | tr ' ' '_')
            key=$(echo $key | tr '.' '_')
            key=$(echo $key | tr '-' '_')
            eval "${key}=\"${value}\""
            #echo -e "[DEBUG] key   =" ${key}
            #echo -e "[DEBUG] value =" ${value}
            # we quote to account for spaces in values
            echo "${key}=\"${value}\"" >> $outfileSource
            echo "\$${key}=\"${value}\";" >> $outfilephp
          fi
        fi
      fi
    done < "$file"
    echo -e "\n[INFO] source properties created in $outfileSource\n"
    echo -e "\n[INFO] php properties created in $outfilephp\n"
  else
    echo -e "\n[ERROR] property file not found at expected location: \n\t$file\n"
  fi
}
propertiesfile="${build_home}/org.eclipse.cbi.p2repo.analyzers/releng/org.eclipse.cbi.p2repo.analyzers.product/target/mavenproperties.properties"
sourceProperties="${build_home}/mavenproperties.shsource"
phpProperties="${build_home}/mavenproperties.php"

convertProperties $propertiesfile $sourceProperties $phpProperties
# we copy up to root, for simpler "archiving". 
cp $propertiesfile ${build_home}/mavenproperties.properites









function deployRepos
{
  newRepo=$1
  builtRepo=$2
  mkdir -p ${newRepo}
  cp -r ${builtRepo}/* ${newRepo}/

  #TODO: add mirror URL, etc.
}

source $sourceProperties

baseDL=/home/data/httpd/download.eclipse.org/cbi/updates/analyzers
ideUpdate=${baseDL}/${updateRelease}/${buildId}
#headlessUpdate=${baseDL}/headless/${updateRelease}/${buildId}

deployRepos ${ideUpdate} ${build_home}/output/p2repo
#deployRepos ${headlessUpdate} ${build_home}/org.eclipse.cbi.p2repo.analyzers/releng/org.eclipse.cbi.p2repo.cli.product/target/repository

# save away "data" from the build, as well as the deployable headless products
#cp ${build_home}/aggr/buildOutput.txt ${ideUpdate}
cp ${sourceProperties} ${ideUpdate}
cp ${propertiesfile} ${ideUpdate}
cp ${phpProperties} ${ideUpdate}

productroot=${build_home}/output/products
cp ${productroot}/org.eclipse.cbi.p2repo.analyzers.product-linux.gtk.x86_64.tar.gz  ${ideUpdate}/org.eclipse.cbi.p2repo.analyzers.product_${buildId}_linux.gtk.x86_64.tar.gz
cp ${productroot}/org.eclipse.cbi.p2repo.analyzers.product-macosx.cocoa.x86_64.tar.gz ${ideUpdate}/org.eclipse.cbi.p2repo.analyzers.product_${buildId}_macosx.cocoa.x86_64.tar.gz
cp ${productroot}/org.eclipse.cbi.p2repo.analyzers.product-win32.win32.x86_64.zip ${ideUpdate}/org.eclipse.cbi.p2repo.analyzers.product_${buildId}_win32.win32.x86_64.zip

rsync -a ${build_home}/output/reporeports ${ideUpdate}/
