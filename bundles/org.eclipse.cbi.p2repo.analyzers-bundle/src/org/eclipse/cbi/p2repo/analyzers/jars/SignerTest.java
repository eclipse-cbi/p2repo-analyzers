/** 
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 */
package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.utils.BundleJarUtils;
import org.eclipse.cbi.p2repo.analyzers.utils.JARFileNameFilter;
import org.eclipse.cbi.p2repo.analyzers.utils.PlainCheckReport;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;
import org.eclipse.cbi.p2repo.analyzers.utils.VerifyStep;

public class SignerTest extends TestJars {
    static final String UNSIGNED_FILENAME = "unsigned8.txt";
    static final String SIGNED_FILENAME = "verified8.txt";
    static final String KNOWN_UNSIGNED = "knownunsigned8.txt";

    public SignerTest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    /**
     * @return <code>true</code> if errors were found 
     */
    boolean verifySignatures() throws IOException {
        Set<PlainCheckReport> checkReports = new CopyOnWriteArraySet<>();
        if (!VerifyStep.canVerify()) {
            System.err.println("jarsigner is not available. Can not check.");
            return true;
        }
        checkJars(new File(getFeatureDirectory()), "feature", checkReports);
        checkJars(new File(getBundleDirectory()), "plugin", checkReports);
        boolean containsErrors = checkReports.stream().anyMatch(report -> report.getType() == ReportType.NOT_IN_TRAIN);
        printSummary(checkReports);
        return containsErrors;
    }

    void checkJars(File dirToCheck, String iuType, Set<PlainCheckReport> reports) {
        File[] jars = dirToCheck.listFiles(new JARFileNameFilter());
        Stream.of(jars).parallel().forEach(new SignerCheck(reports, iuType));
    }

    private void printSummary(Set<PlainCheckReport> reports) throws IOException {
        ReportWriter info = createNewReportWriter(SIGNED_FILENAME);
        ReportWriter warn = createNewReportWriter(KNOWN_UNSIGNED);
        ReportWriter error = createNewReportWriter(UNSIGNED_FILENAME);
        try {
            long featuresCount = reports.stream().filter(report -> report.getIuType().equals("feature")).count();
            info.writeln("Jars checked: " + reports.size() + ". " + featuresCount + " features and " + (reports.size() -featuresCount) + " plugins.");
            info.writeln("Valid signatures: " + reports.stream().filter(report -> report.getType() == ReportType.INFO).count() + ".");
            info.writeln("Explicitly excluded from signing: " + reports.stream().filter(report -> report.getType() == ReportType.BAD_GUY).count() + ". See " + KNOWN_UNSIGNED + " for more details.");
            info.writeln("Invalid or missing signature: " + reports.stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count() + ". See " + UNSIGNED_FILENAME + " for more details.");

            int longestFileName = reports.stream().mapToInt(report -> report.getFileName().length()).max().orElse(0);
            for (PlainCheckReport report : reports.stream().sorted(Comparator.comparing(PlainCheckReport::getFileName)).toArray(PlainCheckReport[]::new)) {
                String indent = " ".repeat(longestFileName - report.getFileName().length());
                String trailing = " ".repeat(10 - report.getIuType().length());
                String line = report.getFileName() + indent + "    " + report.getIuType() + trailing + "   " + report.getCheckResult();
                switch (report.getType()) {
                    case INFO: info.writeln(line); break;
                    case WARNING: info.writeln(line); break;
                    case NOT_IN_TRAIN: error.writeln(line); break;
                    case BAD_GUY: warn.writeln(line); break;
                }
            }
        } finally {
            info.close();
            warn.close();
            error.close();
        }
    }

    protected ReportWriter createNewReportWriter(String filename) {
        return new ReportWriter(getReportOutputDirectory() + "/" + filename);
    }

    final static class SignerCheck implements Consumer<File> {
        final Collection<PlainCheckReport> reports;
        final String iuTypeName;

        SignerCheck(Collection<PlainCheckReport> reports, String iuTypeName) {
            this.reports = reports;
            this.iuTypeName = iuTypeName;
        }

        @Override
        public void accept(File file) {
            PlainCheckReport checkReport = new PlainCheckReport();
            checkReport.setFileName(file.getName());
            checkReport.setIuType(iuTypeName);

            File fileToCheck = file;
            // signing disabled: jarprocessor.exclude.sign = true
            Properties eclipseInf = BundleJarUtils.getEclipseInf(fileToCheck);
            if (Boolean.valueOf(eclipseInf.getProperty("jarprocessor.exclude.sign", "false"))) {
                // skip check
                checkReport.setType(ReportType.BAD_GUY);
                checkReport.setCheckResult("Jar was excluded from signing using the eclipse.inf entry.");
            } else {
                StringBuilder errorOut = new StringBuilder();
                StringBuilder warningOut = new StringBuilder();
                boolean verified = VerifyStep.verify(fileToCheck, errorOut, warningOut);
                if (!verified) {
                    checkReport.setType(ReportType.NOT_IN_TRAIN);
                    checkReport.setCheckResult(errorOut.toString());
                } else {
                    String message = "jar verified";
                    if (warningOut.length() > 0) {
                        checkReport.setType(ReportType.INFO);
                        message = warningOut.toString();
                    }
                    checkReport.setCheckResult(message);
                }
            }
            reports.add(checkReport);
        }

    }
}
