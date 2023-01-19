package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

import org.eclipse.cbi.p2repo.analyzers.BuildRepoTests;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;

public abstract class TestJars extends BuildRepoTests {

    public TestJars(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private String bundleDirectory;
    private String featureDirectory;

    protected String getBundleDirectory() {
        if (bundleDirectory == null) {
            // we try both aggregate/plugins and if doesn't exist, assume
            // plugins
            String AGGR_PLUGINS_DIR = "aggregate/plugins";
            String PLUGINS_DIR = "plugins";
            String property = getDirectoryToCheck();
            if (property == null) {
                handleFatalError("Need to set input directory to check against.");
            }
            property = ensureEndingSlash(property);

            // try aggregate first
            bundleDirectory = property + AGGR_PLUGINS_DIR;
            File inputdir = new File(bundleDirectory);
            if (!(inputdir.exists() && inputdir.isDirectory())) {
                // then try more common non-aggregate plugins directory
                bundleDirectory = property + PLUGINS_DIR;
                inputdir = new File(bundleDirectory);
                if (!(inputdir.exists() && inputdir.isDirectory())) {
                    handleFatalError("bundle directory (" + bundleDirectory + ") must be an existing directory.");
                }
            }
        }
        return bundleDirectory;
    }

    private String ensureEndingSlash(String property) {
        String result = property;
        if (!result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }

    protected String getFeatureDirectory() {
        if (featureDirectory == null) {
            // we try both aggregate/plugins and if doesn't exist, assume
            // plugins
            String AGGR_FEATURES_DIR = "aggregate/features";
            String FEATURES_DIR = "features";
            String property = getDirectoryToCheck();
            if (property == null) {
                handleFatalError("Need to set input directory to check against.");
            }
            property = ensureEndingSlash(property);
            // try aggregate first
            featureDirectory = property + AGGR_FEATURES_DIR;
            File inputdir = new File(featureDirectory);
            if (!(inputdir.exists() && inputdir.isDirectory())) {
                // then try more common non-aggregate plugins directory
                featureDirectory = property + FEATURES_DIR;
                inputdir = new File(featureDirectory);
                if (!(inputdir.exists() && inputdir.isDirectory())) {
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
