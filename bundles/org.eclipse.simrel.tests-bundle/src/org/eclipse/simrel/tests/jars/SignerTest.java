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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.internal.p2.jarprocessor.StreamProcessor;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;
import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.utils.JARFileNameFilter;
import org.eclipse.simrel.tests.utils.ReportWriter;

public class SignerTest extends TestJars {
    private static final String outputFilename = "signing.txt";

    public SignerTest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    // private static final String outputFilename = "signing.txt";

    public boolean verifySignatures() throws IOException {
        final List<String> errors = new CopyOnWriteArrayList<String>();
        final List<String> warnings = new CopyOnWriteArrayList();
        File inputdir = new File(getBundleDirectory());
        File[] jarchildren = inputdir.listFiles(new JARFileNameFilter());
        if (VerifyStep.canVerify()) {
            ExecutorService threadPool = Executors.newFixedThreadPool(64);
            for (final File file : jarchildren) {
                threadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        StringBuilder errorOut = new StringBuilder();
                        StringBuilder warningOut = new StringBuilder();
                        boolean success = VerifyStep.verify(file, errorOut, warningOut);
                        StringBuilder msg = new StringBuilder();
                        msg.append(file.getName());
                        msg.append(": ");
                        if (!success) {
                            errors.add(msg.toString() + " " + errorOut.toString());
                        } else {
                            if (errorOut.length() > 0)
                                errors.add(msg.toString() + " " + errorOut.toString());
                            if (warningOut.length() > 0)
                                warnings.add(msg.toString() + " " + warningOut.toString());
                        }

                    }
                });

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
            reportWriter.writeln("   Number of jar files checked - " + checked);
            reportWriter.writeln("   Errors found: " + errors.size());

            if (errors.size() > 0) {
                List<String> sortable = new ArrayList<String>(errors);
                Collections.sort(sortable);
                for (String error : sortable) {
                    reportWriter.writeln(error);
                }
            }
            reportWriter.writeln();
            reportWriter.writeln("   Warnings found: " + warnings.size());

            if (warnings.size() > 0) {
                List<String> sortable = new ArrayList<String>(errors);
                Collections.sort(sortable);
                for (String warn : sortable) {
                    reportWriter.writeln(warn);
                }
            }
        } finally {
            reportWriter.close();
        }
    }

    public static class VerifyStep {

        private static final String JAR_VERIFIED  = "jar verified.";
        static String               verifyCommand = "jarsigner";    //$NON-NLS-1$
        static Boolean              canVerify     = null;

        public static boolean canVerify() {
            if (canVerify != null)
                return canVerify.booleanValue();

            String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
            String command = javaHome + "/../bin/jarsigner"; //$NON-NLS-1$
            int result = execute(new String[] { command }, false);
            if (result < 0) {
                command = "jarsigner"; //$NON-NLS-1$
                result = execute(new String[] { command }, false);
                if (result < 0) {
                    canVerify = Boolean.FALSE;
                    return false;
                }
            }
            verifyCommand = command;
            canVerify = Boolean.TRUE;
            return true;
        }

        public String getStepName() {
            return "Verify"; //$NON-NLS-1$
        }

        public File postProcess(File input, File workingDirectory, List<Properties> containers) {
            return null;
        }

        public static boolean verify(File input, StringBuilder errors, StringBuilder warnings) {
            if (canVerify() && verifyCommand != null) {
                try {
                    String[] cmd = new String[] { verifyCommand, "-verify", input.getCanonicalPath() }; //$NON-NLS-1$
                    ProcessBuilder procBuilder = new ProcessBuilder(cmd);
                    procBuilder.redirectErrorStream(true);

                    Process resultProc = procBuilder.start();
                    int result = resultProc.waitFor();
                    if (result != 0)
                        errors.append("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$

                    BufferedReader reader = new BufferedReader(new InputStreamReader(resultProc.getInputStream()));
                    StringBuilder out = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        out.append(line);
                    }
                    reader.close();

                    String outString = out.toString();
                    if (!JAR_VERIFIED.equals(outString)) {
                        if (outString.contains("Warning:")) {
                            warnings.append(outString);
                        } else {
                            errors.append(out);
                            return false;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            return false;
        }

        protected static int execute(String[] cmd, boolean verbose) {
            Runtime runtime = Runtime.getRuntime();
            Process proc = null;
            try {
                proc = runtime.exec(cmd);
                StreamProcessor.start(proc.getInputStream(), StreamProcessor.STDOUT, verbose);
            } catch (Exception e) {
                if (verbose) {
                    System.out.println("Error executing command " + Utils.concat(cmd)); //$NON-NLS-1$
                    e.printStackTrace();
                }
                return -1;
            }
            try {
                int result = proc.waitFor();
                return result;
            } catch (InterruptedException e) {
                if (verbose)
                    e.printStackTrace();
            }
            return -1;
        }
    }

}
