package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.eclipse.cbi.p2repo.analyzers.BuildRepoTests;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;

public abstract class TestJars extends BuildRepoTests {

    public TestJars(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private Path   bundleDirectory;
    private Path featureDirectory;

    protected Path getBundleDirectory() {
        if (bundleDirectory == null) {
            // we try both aggregate/plugins and if doesn't exist, assume
            // plugins
            Path property = getDirectoryToCheck();
            if (property == null) {
                handleFatalError("Need to set input directory to check against.");
            }
            // try aggregate first
            bundleDirectory = property.resolve("aggregate/plugins");
            if (!(Files.exists(bundleDirectory) && Files.isDirectory(bundleDirectory))) {
                // then try more common non-aggregate plugins directory
                bundleDirectory = property.resolve("plugins");
                if (!(Files.exists(bundleDirectory) && Files.isDirectory(bundleDirectory))) {
                    handleFatalError("bundle directory (" + bundleDirectory + ") must be an existing directory.");
                }
            }
        }
        return bundleDirectory;
    }


    protected Path getFeatureDirectory() {
        if (featureDirectory == null) {
            // we try both aggregate/plugins and if doesn't exist, assume
            // plugins
            Path property = getDirectoryToCheck();
            if (property == null) {
                handleFatalError("Need to set input directory to check against.");
            }
            // try aggregate first
            featureDirectory = property.resolve("aggregate/features");
            if (!(Files.exists(featureDirectory) && Files.isDirectory(featureDirectory))) {
                // then try more common non-aggregate plugins directory
                featureDirectory = property.resolve("features");
                if (!(Files.exists(featureDirectory) && Files.isDirectory(featureDirectory))) {
                    handleFatalError("feature directory (" + featureDirectory + ") must be an existing directory.");
                }
            }
        }
        return featureDirectory;
    }

    protected void printInvalidJars(List<String> invalidJars, ReportWriter reportWriter) throws FileNotFoundException {
        if (!invalidJars.isEmpty()) {
            reportWriter.writeln("The following jars could not be read, perhaps invalid signatures led to security exceptions?");
            Collections.sort(invalidJars);
            for (String bundle : invalidJars) {
                reportWriter.writeln("       " + bundle);
            }
        }
    }

}
