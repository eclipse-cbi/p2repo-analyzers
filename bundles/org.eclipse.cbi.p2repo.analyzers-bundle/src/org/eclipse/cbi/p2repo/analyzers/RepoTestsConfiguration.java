/**
 *
 */
package org.eclipse.cbi.p2repo.analyzers;

import java.io.File;
import java.net.URI;

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

    private String  referenceRepoDir;
    private String  reportOutputDir;
    private String  reportRepoDir;
    private String  tempWorkingDir;
    private String  repoURLToTest;
    private String  repoURLForReference;

    /**
     * @param reportRepoDir
     * @param reportOutputDir
     * @param referenceRepoDir
     * @param tempWorkingDir
     */
    public RepoTestsConfiguration(String reportRepoDir, String reportOutputDir, String referenceRepoDir, String tempWorkingDir) {
        this.reportRepoDir = reportRepoDir;
        this.reportOutputDir = reportOutputDir;
        this.referenceRepoDir = referenceRepoDir;
        this.tempWorkingDir = tempWorkingDir;
    }

    /**
     * @return the repoURLToTest
     */
    public String getRepoURLToTest() {
        return repoURLToTest;
    }

    /**
     * @param repoURLToTest
     *            the repoURLToTest to set
     */
    public void setRepoURLToTest(String repoURLToTest) {
        this.repoURLToTest = repoURLToTest;
    }

    /**
     * @return the repoURLForReference
     */
    public String getRepoURLForReference() {
        return repoURLForReference;
    }

    /**
     * @param repoURLForReference
     *            the repoURLForReference to set
     */
    public void setRepoURLForReference(String repoURLForReference) {
        this.repoURLForReference = repoURLForReference;
    }

    public String getReferenceRepoDir() {
        return referenceRepoDir;
    }

    @Override
    public String getReportOutputDir() {
        return this.reportOutputDir;
    }

    public String getReportRepoDir() {
        return this.reportRepoDir;
    }

    @Override
    public URI getReportRepoURI() {
        return (new File(getReportRepoDir()).toURI());
    }

    public String getTempWorkingDir() {
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
        return new RepoTestsConfiguration(repoDir, outDir, refRepoDir, tmpDir);
    }

    @Override
    public String getDataOutputDir() {
        File dataDir = new File(reportOutputDir, "data");
        dataDir.mkdirs();
        return dataDir.getAbsolutePath();
    }
}
