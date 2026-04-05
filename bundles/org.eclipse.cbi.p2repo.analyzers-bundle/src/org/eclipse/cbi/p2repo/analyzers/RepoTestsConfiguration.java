/**
 *
 */
package org.eclipse.cbi.p2repo.analyzers;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.cbi.p2repo.analyzers.common.reporter.IP2RepositoryAnalyserConfiguration;

/**
 * @author dhuebner
 */
public final class RepoTestsConfiguration implements IP2RepositoryAnalyserConfiguration {
    /**
     * this is property where users can specify main directory where output goes
     */
    public static final String REPORT_REPO_DIR_PARAM    = "reportRepoDir";
    public static final String REPORT_OUTPUT_DIR_PARAM  = "reportOutputDir";
    public static final String REFERENCE_REPO_PARAM     = "referenceRepo";
    public static final String REPO_URL_PARAM           = "repoURLToTest";
    public static final String REFERENCE_REPO_URL_PARAM = "repoURLForReference";

    private final Path         referenceRepoDir;
    private final Path         reportOutputDir;
    private final Path         reportRepoDir;
    private final Path         tempWorkingDir;
    private URI                repoURLToTest;
    private URI                repoURLForReference;

    /**
     * @param reportRepoDir
     * @param reportOutputDir
     * @param referenceRepoDir
     * @param tempWorkingDir
     */
    public RepoTestsConfiguration(Path reportRepoDir, Path reportOutputDir, Path referenceRepoDir, Path tempWorkingDir) {
        this.reportRepoDir = reportRepoDir;
        this.reportOutputDir = reportOutputDir;
        this.referenceRepoDir = referenceRepoDir;
        this.tempWorkingDir = tempWorkingDir;
    }

    /**
     * @return the repoURLToTest
     */
    public URI getRepoURLToTest() {
        return repoURLToTest;
    }

    /**
     * @param repoURLToTest
     *            the repoURLToTest to set
     */
    public void setRepoURLToTest(URI repoURLToTest) {
        this.repoURLToTest = repoURLToTest;
    }

    /**
     * @return the repoURLForReference
     */
    public URI getRepoURLForReference() {
        return repoURLForReference;
    }

    /**
     * @param repoURLForReference
     *            the repoURLForReference to set
     */
    public void setRepoURLForReference(URI repoURLForReference) {
        this.repoURLForReference = repoURLForReference;
    }

    public Path getReferenceRepoDir() {
        return referenceRepoDir;
    }

    @Override
    public String getReportOutputDir() {
        return this.reportOutputDir.toString();
    }

    public Path getReportRepoDir() {
        return this.reportRepoDir;
    }

    @Override
    public URI getReportRepoURI() {
        return getReportRepoDir().toUri();
    }

    public Path getTempWorkingDir() {
        return this.tempWorkingDir;
    }

    public static RepoTestsConfiguration createFromSystemProperties() {
        String repoDir = System.getProperty(REPORT_REPO_DIR_PARAM, null);
        if (repoDir == null || repoDir.isEmpty()) {
            repoDir = System.getenv(REPORT_REPO_DIR_PARAM);
        }
        String outDir = System.getProperty(REPORT_OUTPUT_DIR_PARAM, null);
        if (outDir == null || outDir.isEmpty()) {
            outDir = System.getenv(REPORT_OUTPUT_DIR_PARAM);
        }
        String tmpDir = System.getProperty("java.io.tmpdir");
        String refRepoDir = System.getProperty(REFERENCE_REPO_PARAM, null);
        return new RepoTestsConfiguration(Path.of(repoDir), Path.of(outDir), Path.of(refRepoDir), Path.of(tmpDir));
    }

    @Override
    public String getDataOutputDir() {
        Path dataDir = reportOutputDir.resolve("data");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) { // Ignore
        }
        return dataDir.toAbsolutePath().toString();
    }
}
