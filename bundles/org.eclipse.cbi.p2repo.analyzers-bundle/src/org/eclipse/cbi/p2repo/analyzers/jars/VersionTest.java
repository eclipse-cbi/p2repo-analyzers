/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 ******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.FullJarNameParser;
import org.eclipse.cbi.p2repo.analyzers.utils.JARFileNameFilter;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;

/**
 * @since 3.3
 */
public class VersionTest extends TestJars {

    public VersionTest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private static final String outputFilename       = "versionPatternCheck.txt";
    private static final String EXTENSION_JAR        = ".jar";
    private static final String EXTENSION_ZIP        = ".zip";
    private FullJarNameParser   nameParser           = new FullJarNameParser();
    private String              BACKSLASH            = "\\";
    private String              LITERAL_PERIOD       = BACKSLASH + ".";
    private String              ANY                  = ".*";
    private Pattern             threedots            = Pattern.compile(ANY + LITERAL_PERIOD + ANY + LITERAL_PERIOD + ANY
                                                             + LITERAL_PERIOD + ANY);
    public ReportWriter         reportWriter;

    public static void main(String[] args) {

        VersionTest testlayout = new VersionTest(RepoTestsConfiguration.createFromSystemProperties());
        testlayout.setDirectoryToCheck("D:\\temp\\staging\\");
        try {
            testlayout.testFeatureVersions();
            testlayout.testBundleVersions();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean testBundleVersions() throws IOException {

        File inputdir = new File(getBundleDirectory());

        getReportWriter().writeln(" Check for 4-part versions in Bundles");

        // assertFalse("Some bundles did not have 4 part version numbers",
        // failuresOccured);
        return checkFilesVersions(inputdir);
    }

    private boolean checkFilesVersions(File inputdir) throws IOException {
        // reset/initialize errors
        List errors = new ArrayList();
        boolean failuresOccured = false;
        File[] children = inputdir.listFiles(new JARFileNameFilter());
        int totalsize = children.length;
        int checked = 0;
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String name = child.getName();
            // assume directory if not file
            if (child.isFile()) {
                if (name.endsWith(EXTENSION_JAR)) {
                    name = getBasicName(name, EXTENSION_JAR);
                } else if (name.endsWith(EXTENSION_ZIP)) {
                    name = getBasicName(name, EXTENSION_ZIP);
                }
            }
            checked++;
            boolean valid = nameParser.parse(name);
            if (!valid) {
                errors.add(name + " does not have a valid version (it is unparsable)");
            } else {
                String version = nameParser.getVersionString();
                Matcher matcher = threedots.matcher(version);

                if (!matcher.matches()) {
                    errors.add(name + " does not contain 4 parts");
                }
            }
        }
        getReportWriter().writeln("   Checked " + checked + " of " + totalsize + ".");
        getReportWriter().writeln("   Errors found: " + errors.size());

        if (errors.size() > 0) {
            Collections.sort(errors);
            for (Iterator iter = errors.iterator(); iter.hasNext();) {
                getReportWriter().writeln(iter.next());
            }
            failuresOccured = true;
        }
        return failuresOccured;
    }

    private String getBasicName(String fullname, String extension) {
        String result = fullname;
        int pos = fullname.lastIndexOf(extension);
        if (pos >= 0) {
            result = fullname.substring(0, pos);
        }
        return result;
    }

    public boolean testVersionsPatterns() throws IOException {
        boolean result = false;
        createReportWriter(outputFilename);
        try {
            getReportWriter().writeln("Check 4-part version patterns");
            boolean featureFailures = testFeatureVersions();
            boolean bundleFailures = testBundleVersions();
            result = featureFailures || bundleFailures;
        } finally {
            getReportWriter().close();
        }
        return result;
    }

    private boolean testFeatureVersions() throws IOException {

        File inputdir = new File(getFeatureDirectory());

        getReportWriter().writeln(" Check for 4-part versions in Features");

        // assertFalse("Some features did not have 4 part version numbers",
        // failuresOccured);
        return checkFilesVersions(inputdir);

    }
}
