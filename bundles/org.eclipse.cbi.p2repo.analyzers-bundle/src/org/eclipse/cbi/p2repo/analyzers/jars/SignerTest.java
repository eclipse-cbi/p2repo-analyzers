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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.repos.TestRepo;
import org.eclipse.cbi.p2repo.analyzers.utils.BundleJarUtils;
import org.eclipse.cbi.p2repo.analyzers.utils.PlainCheckReport;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;
import org.eclipse.cbi.p2repo.analyzers.utils.VerifyStep;
// import org.eclipse.cbi.p2repo.analyzers.utils.VerifyStep;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.osgi.signedcontent.SignedContentFactory;
import org.osgi.framework.FrameworkUtil;

public class SignerTest extends TestJars {
    static final String           UNSIGNED_FILENAME = "unsigned8.txt";
    static final String           SIGNED_FILENAME   = "verified8.txt";
    static final String           KNOWN_UNSIGNED    = "knownunsigned8.txt";

    final SignedContentFactory    verifierFactory   = ServiceHelper
            .getService(FrameworkUtil.getBundle(SignerTest.class).getBundleContext(), SignedContentFactory.class);
    final IFileArtifactRepository artifactRepository;
    boolean                       useJarsigner;

    public SignerTest(RepoTestsConfiguration configurations) {
        super(configurations);
        useJarsigner = "true".equals(System.getProperty("useJarsigner"));
        try {
            artifactRepository = (IFileArtifactRepository) TestRepo.getAgent().getService(IArtifactRepositoryManager.class)
                    .loadRepository(new File(configurations.getReportRepoDir()).toURI(), new NullProgressMonitor());
        } catch (ProvisionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @return <code>true</code> if errors were found
     */
    public boolean verifySignatures() throws IOException {
        Set<PlainCheckReport> checkReports = new CopyOnWriteArraySet<>();
        if (useJarsigner && !VerifyStep.canVerify()) {
            System.err.println("jarsigner is not available. Can not check.");
            return true;
        }
        var artifactKeys = artifactRepository.query(ArtifactKeyQuery.ALL_KEYS, null);
        var descriptors = StreamSupport.stream(artifactKeys.spliterator(), false).map(artifactRepository::getArtifactDescriptors)
                .map(Arrays::asList).flatMap(Collection::stream).collect(Collectors.toList());
        descriptors.parallelStream().forEach(new SignerCheck(checkReports));

        boolean containsErrors = checkReports.stream().anyMatch(report -> report.getType() == ReportType.NOT_IN_TRAIN);
        printSummary(checkReports);
        return containsErrors;
    }

    private void printSummary(Set<PlainCheckReport> reports) throws IOException {
        ReportWriter info = createNewReportWriter(SIGNED_FILENAME);
        ReportWriter warn = createNewReportWriter(KNOWN_UNSIGNED);
        ReportWriter error = createNewReportWriter(UNSIGNED_FILENAME);
        try {
            long featuresCount = reports.stream().filter(report -> report.getIuType().equals("feature")).count();
            info.writeln("Jars checked: " + reports.size() + ". " + featuresCount + " features and "
                    + (reports.size() - featuresCount) + " plugins.");
            info.writeln(
                    "Valid signatures: " + reports.stream().filter(report -> report.getType() == ReportType.INFO).count() + ".");
            info.writeln("Explicitly excluded from signing: "
                    + reports.stream().filter(report -> report.getType() == ReportType.BAD_GUY).count() + ". See " + KNOWN_UNSIGNED
                    + " for more details.");
            info.writeln("Invalid or missing signature: "
                    + reports.stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count() + ". See "
                    + UNSIGNED_FILENAME + " for more details.");

            int longestFileName = reports.stream().mapToInt(report -> report.getFileName().length()).max().orElse(0);
            for (PlainCheckReport report : reports.stream().sorted(Comparator.comparing(PlainCheckReport::getFileName))
                    .toArray(PlainCheckReport[]::new)) {
                String indent = " ".repeat(longestFileName - report.getFileName().length());
                String trailing = " ".repeat(10 - report.getIuType().length());
                String line = report.getFileName() + indent + "    " + report.getIuType() + trailing + "   "
                        + report.getCheckResult();
                switch (report.getType()) {
                case INFO:
                    info.writeln(line);
                    break;
                case WARNING:
                    info.writeln(line);
                    break;
                case NOT_IN_TRAIN:
                    error.writeln(line);
                    break;
                case BAD_GUY:
                    warn.writeln(line);
                    break;
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

    final class SignerCheck implements Consumer<IArtifactDescriptor> {
        final Collection<PlainCheckReport> reports;

        SignerCheck(Collection<PlainCheckReport> reports) {
            this.reports = reports;
        }

        @Override
        public void accept(IArtifactDescriptor descriptor) {
            PlainCheckReport checkReport = new PlainCheckReport();

            File file = artifactRepository.getArtifactFile(descriptor);

            if (descriptor.getProcessingSteps().length > 0) {
                return;
            }

            String classifier = descriptor.getArtifactKey().getClassifier();

            String iuTypeName = "osgi.bundle".equals(classifier) ? "plugin"
                    : "org.eclipse.update.feature".equals(classifier) ? "feature" : null;
            if (iuTypeName == null) {
                return;
            }

            checkReport.setFileName(file.getName());
            checkReport.setIuType(iuTypeName);

            // signing disabled: jarprocessor.exclude.sign = true
            Properties eclipseInf = BundleJarUtils.getEclipseInf(file);
            if (Boolean.parseBoolean(eclipseInf.getProperty("jarprocessor.exclude.sign", "false"))) {
                // skip check
                checkReport.setType(ReportType.BAD_GUY);
                checkReport.setCheckResult("Jar was excluded from signing using the eclipse.inf entry.");
            } else {
                StringBuilder errorOut = new StringBuilder();
                StringBuilder warningOut = new StringBuilder();
                boolean signed;
                if (useJarsigner) {
                    signed = VerifyStep.verify(file, errorOut, warningOut);
                } else {
                    try {
                        signed = verifierFactory.getSignedContent(file).isSigned();
                    } catch (Exception ex) {
                        errorOut.append(ex.getMessage());
                        signed = false;
                    }
                }
                if (signed) {
                    String message = "jar verified (jarsigner)";
                    if (warningOut.length() > 0) {
                        checkReport.setType(ReportType.INFO);
                        message = warningOut.toString();
                    }
                    checkReport.setCheckResult(message);
                } else if (PGPVerifier.verify(descriptor, file, artifactRepository, errorOut, warningOut)) {
                    String message = "artifact verified (pgp signatures)";
                    if (warningOut.length() > 0) {
                        checkReport.setType(ReportType.INFO);
                        message = warningOut.toString();
                    }
                    checkReport.setCheckResult(message);
                } else {
                    checkReport.setType(ReportType.NOT_IN_TRAIN);
                    checkReport.setCheckResult(errorOut.toString());
                }
            }
            reports.add(checkReport);
        }
    }
}
