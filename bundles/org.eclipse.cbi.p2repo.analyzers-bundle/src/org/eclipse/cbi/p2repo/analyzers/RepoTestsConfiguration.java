/**
 *
 */
package org.eclipse.cbi.p2repo.analyzers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

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

    private final URI          referenceRepo;
    private final Path         reportOutputDir;
    private final Path         reportRepoDir;
    private final Path         tempWorkingDir;
    private URI                repoURLToTest;
    private URI                repoURLForReference;

    /**
     * @param reportRepoDir
     * @param reportOutputDir
     * @param referenceRepo
     * @param tempWorkingDir
     */
    public RepoTestsConfiguration(Path reportRepoDir, Path reportOutputDir, URI referenceRepo, Path tempWorkingDir) {
        this.reportRepoDir = reportRepoDir;
        this.reportOutputDir = reportOutputDir;
        this.referenceRepo = referenceRepo;
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

    public URI getReferenceRepo() {
        return referenceRepo;
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
        Path repoDir = getSystemOrEnvValue(REPORT_REPO_DIR_PARAM).orElse(null);
        Path outDir = getSystemOrEnvValue(REPORT_OUTPUT_DIR_PARAM).orElse(null);
        Path tmpDir = Path.of(System.getProperty("java.io.tmpdir"));
        URI referenceRepo = parsePathToURI(System.getProperty(REFERENCE_REPO_PARAM, null));
        return new RepoTestsConfiguration(repoDir, outDir, referenceRepo, tmpDir);
    }

    private static Optional<Path> getSystemOrEnvValue(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            value = System.getenv(key);
        }
        return Optional.ofNullable(value).map(Path::of);
    }

    private static URI parsePathToURI(String refRepoDir) {
        try {
            URI referenceRepo = new URI(refRepoDir);
            if (referenceRepo.getScheme() != null) {
                return referenceRepo;
            } // a null scheme was probably a UNIX path
        } catch (URISyntaxException e) { // assume file path
        }
        return Path.of(refRepoDir).toUri();
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
