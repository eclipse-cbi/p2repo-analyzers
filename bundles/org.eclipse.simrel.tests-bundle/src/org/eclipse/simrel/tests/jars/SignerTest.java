/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 ******************************************************************************/
package org.eclipse.simrel.tests.jars;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.utils.BundleJarUtils;
import org.eclipse.simrel.tests.utils.CompositeFileFilter;
import org.eclipse.simrel.tests.utils.JARFileNameFilter;
import org.eclipse.simrel.tests.utils.PackGzFileNameFilter;
import org.eclipse.simrel.tests.utils.ReportWriter;
import org.eclipse.simrel.tests.utils.VerifyStep;

public class SignerTest extends TestJars {
    private static final String outputFilename = "signing.txt";

    public SignerTest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    public boolean verifySignatures() throws IOException {
        final List<String> errors = new CopyOnWriteArrayList<String>();
        final List<String> warnings = new CopyOnWriteArrayList();
        File inputdir = new File(getBundleDirectory());
        File[] jarchildren = inputdir.listFiles(CompositeFileFilter.create(new JARFileNameFilter(), new PackGzFileNameFilter()));
        if (VerifyStep.canVerify()) {
            ExecutorService threadPool = Executors.newFixedThreadPool(64);
            for (final File file : jarchildren) {
                threadPool.execute(new SignerRunnable(errors, warnings, file));
            }
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.MINUTES)) {
                    threadPool.shutdownNow(); // Cancel currently executing
                                              // tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!threadPool.awaitTermination(5, TimeUnit.MINUTES))
                        System.err.println("ThreadPool did not terminate");
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                threadPool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        } else {
            errors.add("jarsigner is not available. Can not check.");
        }
        printSummary(errors, warnings, jarchildren.length);
        return errors.size() > 0;
    }

    private void printSummary(List<String> errors, List<String> warnings, int checked) throws IOException {

        ReportWriter reportWriter = createReportWriter(outputFilename);
        try {
            reportWriter.writeln();
            reportWriter.writeln("Directory checked: " + getBundleDirectory());
            reportWriter.writeln();
            reportWriter.writeln("Check of signed bundles.");
            String indentation = "   ";
            reportWriter.writeln(indentation + "Number of files checked - " + checked);
            reportWriter.writeln(indentation + "Errors found: " + errors.size());

            if (errors.size() > 0) {
                List<String> sortable = new ArrayList<String>(errors);
                Collections.sort(sortable);
                for (String error : sortable) {
                    reportWriter.writeln(indentation + error);
                }
            }
            reportWriter.writeln();
            reportWriter.writeln(indentation + "Warnings found: " + warnings.size());

            if (warnings.size() > 0) {
                List<String> sortable = new ArrayList<String>(warnings);
                Collections.sort(sortable);
                for (String warn : sortable) {
                    reportWriter.writeln(indentation + warn);
                }
            }
        } finally {
            reportWriter.close();
        }
    }

    final class SignerRunnable implements Runnable {
        private final List<String> errors;
        private final List<String> warnings;
        private final File         file;

        SignerRunnable(List<String> errors, List<String> warnings, File file) {
            this.errors = errors;
            this.warnings = warnings;
            this.file = file;
        }

        @Override
        public void run() {
            File fileToCheck = file;
            StringBuilder errorOut = new StringBuilder();
            StringBuilder warningOut = new StringBuilder();
            if (fileToCheck.getName().endsWith(PackGzFileNameFilter.EXTENSION_PACEKD_JAR)) {
                try {
                    fileToCheck = BundleJarUtils.unpack200gz(fileToCheck);
                } catch (IOException e) {
                    errors.add("Unable to unpack " + file.getAbsolutePath() + ". Can not check signature. " + e.getMessage());
                }
            }
            boolean success = false;
            StringBuilder msg = new StringBuilder();
            msg.append(file.getName());
            msg.append(": ");
            // signing disabled: jarprocessor.exclude.sign = true
            Properties eclipseInf = BundleJarUtils.getEclipseInf(fileToCheck);
            if (Boolean.valueOf(eclipseInf.getProperty("jarprocessor.exclude.sign", "false"))) {
                // skip check
                success = true;
                warningOut.append("Jar was excluded from signing using the eclipse.inf entry.");
            } else {
                success = VerifyStep.verify(fileToCheck, errorOut, warningOut);
            }

            if (!success) {
                errors.add(msg.toString() + " " + errorOut.toString());
            } else {
                if (errorOut.length() > 0)
                    errors.add(msg.toString() + " " + errorOut.toString());
                if (warningOut.length() > 0)
                    warnings.add(msg.toString() + " " + warningOut.toString());
            }
        }
    }

}
