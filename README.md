# <img src="https://raw.githubusercontent.com/eclipse-cbi/p2repo-aggregator/main/cbi.svg" style="width: 1em;"/> p2 Repository Analyzer

The CBI repository analyzer is built by 
[https://ci.eclipse.org/cbi/view/p2RepoRelated/job/p2repo-analyzers/](https://ci.eclipse.org/cbi/view/p2RepoRelated/job/p2repo-analyzers/).

This job's builds produce updates sites for the 
[tools](https://download.eclipse.org/cbi/updates/p2-analyzers/tools/) and the 
[products](https://download.eclipse.org/cbi/updates/p2-analyzers/products/).

## Goals

The p2RepoAnalyzers are a collection of automated quality (legal, version rules...) checks and reports to run against p2 repositories or directories of jars. They're meant to be used by Eclipse projects to guarantee conformance to typical Eclipse.org rules.

**The main goal is that all projects can perform these tests themselves, early in development cycle, every time they build a p2 repository.**

So far, the main user of these reports is the SimRel Build: the reports for the simultaneous release repository are simply a final sanity check. They are ran against the latest successful Simultaneous Release aggregator build in [this job](https://ci.eclipse.org/simrel/job/simrel.reporeports.pipeline/). There are links on the [SimRel CI instance main page](https://ci.eclipse.org/simrel/) to the report for the latest release (e.g. [2023-09](https://download.eclipse.org/releases/2023-09/202309131000/buildInfo/reporeports/)) and also the latest staging build (e.g. [2023-12](https://download.eclipse.org/staging/2023-12/buildInfo/reporeports/)). 

While several projects have their own, similar tests (which by one way or another have provided the starting point for all these tests) it is worth some effort to collect some common tests in a common place to encourage reuse and improvements. So if you've set up some similar checks for your project, you should highly consider contributing them to this common place.

The reports can be ran locally from your workbench or adopted for your own production builds.

## Description of tests and reports

There are some tests, that look at jars specifically, that require the jars to be on local file system and essentially use plain 'ol Java file IO and regex-type checks on the contents.

Another class of tests, read the content.jar/xml meta-data and reports on the data or relationships in that meta data.

Yet another, small class of tests, verify that the jars are signed. These tests are not really "Java" or "workspace" related, but use Java's "exec" method to invoke `jarsigner -verify` on multiple threads, on a directory of jars. (There is actually a faster heuristic in the code that simply looks for the presence of the Eclipse signature file, but that is a heuristic and might not always be accurate, so it is not used by default).

The code and scripts, as of right now, are oriented towards simply producing reports. If you browse the code, you'll see some commented-out code in places that can cause failed flags to be set, which would be appropriate for most projects and, eventually, even the Simultaneous Release repository. But more recently Dennis Huebner has contributed the framework to run as JUnit tests but that still needs to be documented. See also the `-useNewApi` flag which produces reports based on passed, failed, or warning (which is a variant of passed more than a variant of failed). Follow [bug 487409](https://bugs.eclipse.org/bugs/show_bug.cgi?id=487409) for updating the documentation.

## Running reports for your project

The project is built and delivered as both an Eclipse "product" and as a feature installable from a p2 repository. The Jenkins builds and functional testing is done under https://ci.eclipse.org/cbi/view/p2RepoRelated/ and the p2 repository where the product and feature are currently published is https://download.eclipse.org/cbi/updates/analyzers/4.7/ .

The Eclipse Application is named `org.eclipse.cbi.p2repo.analyzers.repoReport`. It is parameterized by two mandatory System Properties; one to specify where you want the output to go, and another to specify where the repository-to-analyze is on the file system. An optional third parameter names a repository to use as reference for the "version check" reports. For example:

 * `-DreportOutputDir=/home/shared/eclipse/repoReport`
 * `-DreportRepoDir=/home/www/html/downloads/eclipse/updates/4.6-M-builds/M20161013-0730/`
 * `-DreferenceRepo=/home/www/html/downloads/eclipse/updates/4.6/R-4.6.1-201609071200/`

There is another parameter, `-DuseNewApi=true` which is not yet documented ([bug 487409](https://bugs.eclipse.org/bugs/show_bug.cgi?id=487409)) but runs the code in such a way that tests pass, fail, or give a warning, and produces [compact, color coded table of results](http://download.eclipse.org/eclipse/downloads/drops4/R-4.6.1-201609071200/buildlogs/errors-and-moderate_warnings.html), to link to an experimental example.

## In Eclipse IDE

Get the source code from GitHub here [https://github.com/eclipse-cbi/p2repo-analyzers.git](https://github.com/eclipse-cbi/p2repo-analyzers.git). Once you load that project into your workspace, it will include one "launch configuration" that can be used as a starting example, edited and used to launch the application from your workspace.

## With Eclipse command-line

For one example of a bash script that takes advantage of a "product build" to run the report application, see the example in the Eclipse Platform Git repository named [createReports.sh](https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/tree/production/createReports.sh?h=R4_10_maintenance) (outdated).

## With Ant

Running the reports are, as of right now, oriented towards being a simple "Ant task".

For an example of installing as a feature from a p2 repository see the ['installTestsFromRepo' target in the SimRel build.xml file](https://github.com/eclipse-simrel/simrel.tools/blob/main/build.xml#L110). And then, see the actual running of the tests in the ['runReports' target in the SimRel runTests.xml file](https://github.com/eclipse-simrel/simrel.tools/blob/main/runTests.xml#L101).

## With Maven

There is a bug open to convert to Maven tasks (see [bug 487468](https://bugs.eclipse.org/bugs/show_bug.cgi?id=487468)).

However, the application can already be used in a Tycho build, typically as a `verify` step when building an `eclipse-repository`. See example:

```
<!-- [...] -->
<build>
  <plugins>
    <plugin>
      <groupId>org.eclipse.tycho.extras</groupId>
      <artifactId>tycho-eclipserun-plugin</artifactId>
      <version>${tycho-version}</version>
      <executions>
        <execution>
  	  <phase>verify</phase>
  	  <goals>
  	    <goal>eclipse-run</goal>
  	  </goals>
  	  <configuration>
  	    <applicationsArgs>
  	      <arg>-application</arg>
  	      <arg>org.eclipse.cbi.p2repo.analyzers.repoReport</arg>
  	    </applicationsArgs>
  	    <jvmArgs>
  	      <arg>-DreportRepoDir=${project.build.directory}/repository</arg>
  	      <arg>-DreportOutputDir=${project.build.directory}/repository/buildInfo</arg>
	      <arg>-DreferenceRepo=/home/data/httpd/download.eclipse.org/staging/oxygen/</arg>
  	    </jvmArgs>
  	    <executionEnvironment>JavaSE-1.8</executionEnvironment>
  	    <dependencies>
	      <dependency>
		<artifactId>org.eclipse.cbi.p2repo.analyzers</artifactId>
		<type>eclipse-plugin</type>
	      </dependency>
	      <dependency>
	        <artifactId>org.eclipse.equinox.p2.core.feature</artifactId>
		<type>eclipse-feature</type>
	      </dependency>
	      <dependency>
	        <artifactId>org.eclipse.e4.rcp</artifactId>
		<type>eclipse-feature</type>
	      </dependency>
	    </dependencies>
  	    <repositories>
  	      <repository>
  	        <id>cbi-analyzers</id>
  		<url>https://download.eclipse.org/cbi/updates/analyzers/4.7/</url>
  		<layout>p2</layout>
  	      </repository>
  	      <repository>
  	        <id>eclipse-4.7.3</id>
  		<url>https://download.eclipse.org/eclipse/updates/4.7/R-4.7.3-201803010715/</url>
  		<layout>p2</layout>
  	      </repository>
  	    </repositories>
  	  </configuration>
  	</execution>
      </executions>
    </plugin>
  </plugins>
</build>
<!--[...]-->
```

## Notes

On a large repository, it can take 5 or 10 minutes to complete. The results can be viewed with a web browser starting with the `index.html` at the location you specified in the 'reportOutputDir' property.

## Get support

You can ask questions on the [cbi-dev mailing list (cbi-dev@eclipse.org)](https://www.eclipse.org/lists/cbi-dev/index.html) if the tests do not work as expected. 

### Reporting issues

Please report issues on this repository's [issue list](https://github.com/eclipse-cbi/p2repo-analyzers/issues).

### Developing and Contributing

Contributions are always welcome!
See [CONTRIBUTING.md](CONTRIBUTING.md) for details.
