package org.eclipse.cbi.p2repo.analyzers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.eclipse.cbi.p2repo.analyzers.jars.BREETest;
import org.eclipse.cbi.p2repo.analyzers.jars.ESTest;
import org.eclipse.cbi.p2repo.analyzers.jars.SignerTest;
import org.eclipse.cbi.p2repo.analyzers.jars.TestLayoutTest;
import org.eclipse.cbi.p2repo.analyzers.jars.VersionTest;
//import org.eclipse.cbi.p2repo.analyzers.repos.CheckGreedy;
import org.eclipse.cbi.p2repo.analyzers.repos.FeatureDisplayableDataChecker;
import org.eclipse.cbi.p2repo.analyzers.repos.FeatureNameLengths;
import org.eclipse.cbi.p2repo.analyzers.repos.IUNameChecker;
import org.eclipse.cbi.p2repo.analyzers.repos.IUVersionCheckToReference;
import org.eclipse.cbi.p2repo.analyzers.repos.ProviderNameChecker;
import org.eclipse.cbi.p2repo.analyzers.repos.VersionChecking;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;

/**
 * Highest super class of common repo and build directory tests. Should be only
 * the most common methods here or constants here, such as were to put output.
 *
 * @author davidw
 *
 */
public class BuildRepoTests {

    /**
     * the top directory is where high level files go, such as "index.html"
     * which would then relatively point to "reports" directory.
     */
    private static final String TOP_OUTPUT_DIR        = "reporeports";
    /**
     * The sub directory is there most actual reports should go.
     */
    private static final String REPORT_SUB_OUTPUT_DIR = "reports";
    /*
     * under the main output directory, we create (and delete if exists)
     * 'reporeports and 'reporeports/reports'. we assume the reporeports
     * directory is "ours" to delete, etc. TODO: can put in safety checks, in
     * future, to have marker file for safety, and if marker file not there,
     * assume it is not really our directory, not remove it, and print error
     * message to specify another, or manually remove it if simply left over
     * from previous failed run.
     */
    private String              mainoutputDirectory;
    private String              topreportdir;
    private String              reportOutputDirectoryName;

    /**
     * variable to catch errors in use. initializing directory should only be
     * done once, per session or will end up deleting reports generated earlier
     * in session.
     *
     * @return
     */
    private static boolean               outputDirectoryInitialized;
    private String                       directoryToCheck;
    private String                       referenceDirectoryToCheck;
    private String                       tempWorkingDir;
    private boolean                      failuresOccurred = false;
    private ReportWriter                 reportWriter;
    private final RepoTestsConfiguration configurations;

    public BuildRepoTests(final RepoTestsConfiguration configurations) {
        this.configurations = configurations;
    }

    private String getMainOutputDirectory() {
        if (mainoutputDirectory == null) {
            mainoutputDirectory = configurations.getReportOutputDir();
        }
        // if still null or empty, fall back to reasonable "test" location
        // remembering this "site" might be removed in future runs
        if (mainoutputDirectory == null) {
            mainoutputDirectory = System.getProperty("user.home") + "/temp/p2repo";
            System.out.println(
                    "WARNING: no output directory explicitly set, so assumed to be based off user.home: " + mainoutputDirectory);
        }

        return mainoutputDirectory;
    }

    public void setMainOutputDirectory(String mainoutputDirectoryparam) {
        mainoutputDirectory = mainoutputDirectoryparam;
    }

    /**
     * Normally called once, by clients, after setting MainOutputDirectory. It
     * created directories, if they don't exist, and removes any previous
     * content, if they do exist.
     */
    private void initReportOutputDirectory(String mainreportdirname) {
        if (!outputDirectoryInitialized) {

            boolean success = false;
            File topReportDir = new File(mainreportdirname);
            if (topReportDir.exists()) {
                success = removeDirectory(topReportDir);
                if (!success) {
                    handleFatalError("could not remove top level report directory: " + topReportDir);
                }
            }
            String reportDirName = getReportOutputDirectory();
            File reportDir = new File(reportDirName);
            success = reportDir.mkdirs();
            if (!success) {
                handleFatalError("could not create report directory: " + topReportDir);
            }
            try {
                copyTemplateForIndexFile("/templateFiles/indexPending.html");
            } catch (IOException e) {
                handleFatalError("could not copy indexPending.html file into place. " + e.getMessage());
            }
            outputDirectoryInitialized = true;
        }

    }

    private boolean removeDirectory(File dir) {
        boolean result = true;
        String[] dirlist = dir.list();
        for (String childentry : dirlist) {
            File child = new File(dir, childentry);
            if (child.isDirectory()) {
                result = removeDirectory(child);
                if (!result) {
                    break;
                }
            } else {
                result = child.delete();
                if (!result) {
                    break;
                }
            }
        }
        if (result) {
            // should be empty now
            result = dir.delete();
        }
        return result;
    }

    /**
     * should be rarely used, just for copying in index, marker files, etc.
     *
     * @return
     */
    protected String getTopReportOutputDirectory() {
        if (topreportdir == null) {
            String maindir = getMainOutputDirectory();
            topreportdir = maindir + "/" + TOP_OUTPUT_DIR;
            initReportOutputDirectory(topreportdir);
        }

        return topreportdir;
    }

    /**
     * This is the main directory where report generators would put their output
     *
     * @return
     */
    public String getReportOutputDirectory() {
        if (reportOutputDirectoryName == null) {
            reportOutputDirectoryName = getTopReportOutputDirectory() + "/" + REPORT_SUB_OUTPUT_DIR;
        }
        return reportOutputDirectoryName;
    }

    /**
     * common method of halting if something really odd is encountered.
     *
     * @param messagestring
     */
    protected void handleFatalError(String messagestring) {
        System.out.println("ERROR: " + messagestring);
        throw new RuntimeException(messagestring);

    }

    protected void handleWarning(String messagestring) {
        System.out.println("WARNING: " + messagestring);

    }

    public boolean execute() {

        try {
            // these 'do' methods can set failuresOccurred to true
            doRepoTests();

            doDirectoryTests();

            // since signing test is relatively quick, now, we no longer
            // use the "pre-sign" indexmain file. The "pending" index file
            // is copied into place when output directory is first created.
            copyTemplateForIndexFile("/templateFiles/indexmain.html");

        } catch (ProvisionException | OperationCanceledException | IOException | URISyntaxException e) {
            e.printStackTrace();
            failuresOccurred = true;
        }
        return failuresOccurred;
    }

    private void doDirectoryTests() throws IOException {
        ESTest esTest = new ESTest(getConfigurations());
        esTest.setDirectoryToCheck(getDirectoryToCheck());
        esTest.setTempWorkingDir(getTempWorkingDir());

        boolean esFailures = esTest.testESSettingRule();

        if (esFailures) {
            setFailuresOccurred(true);
        }

        BREETest breeTest = new BREETest(getConfigurations());
        breeTest.setDirectoryToCheck(getDirectoryToCheck());
        breeTest.setTempWorkingDir(getTempWorkingDir());

        boolean breeFailures = breeTest.testBREESettingRule();

        if (breeFailures) {
            setFailuresOccurred(true);
        }

        SignerTest signerTest = new SignerTest(getConfigurations());
        signerTest.setDirectoryToCheck(getDirectoryToCheck());
        boolean signFailures = signerTest.verifySignatures();
        if (signFailures) {
            setFailuresOccurred(true);
        }

        VersionTest versionTest = new VersionTest(getConfigurations());
        versionTest.setDirectoryToCheck(getDirectoryToCheck());
        boolean versionCheck = versionTest.testVersionsPatterns();
        if (versionCheck) {
            setFailuresOccurred(true);
        }

        TestLayoutTest test = new TestLayoutTest(getConfigurations());
        test.setDirectoryToCheck(getDirectoryToCheck());
        test.setTempWorkingDir(getTempWorkingDir());
        boolean layoutFailures = test.testLayout();
        if (layoutFailures) {
            setFailuresOccurred(true);
        }
    }

    private void doRepoTests() throws IOException, ProvisionException, OperationCanceledException, URISyntaxException {
        String repoToTest = "file://" + getDirectoryToCheck();
        String referenceRepoToTest = null;
        if (getDirectoryToCheckForReference() != null) {
            File refRepoToCheck = new File(getDirectoryToCheckForReference());
            if (refRepoToCheck.exists()) {
                referenceRepoToTest = "file://" + getDirectoryToCheckForReference();
            } else {
                System.out.println("WARNING: the reference repository was found not to exist. No check done.");
                System.out.println("         referenceRepo: " + getDirectoryToCheckForReference());
            }
        }

        boolean uniquenessCheck = false;
        boolean featureNameFailures = false;
        boolean bundleNameFailures = false;
        boolean providerNamesFailure = false;
        boolean licenseConsistencyFailure = false;
        boolean greedyCheck = false;

        VersionChecking uniquenessChecker = new VersionChecking(getConfigurations());
        uniquenessChecker.setRepoURLToTest(repoToTest);
        uniquenessCheck = uniquenessChecker.testVersionUniqness();
        if (uniquenessCheck) {
            setFailuresOccurred(true);
        }

        IUNameChecker iuNames = new IUNameChecker(getConfigurations());
        iuNames.setRepoURLToTest(repoToTest);
        featureNameFailures = iuNames.testFeatureNames();
        bundleNameFailures = iuNames.testBundleNames();

        // Bug 424376 - repo reports fails to run on latest staging contents
        // CheckGreedy checkGreedy = new CheckGreedy();
        // checkGreedy.setRepoURLToTest(repoToTest);
        // checkGreedy.setDirectoryToCheck(getDirectoryToCheck());
        // greedyCheck = checkGreedy.testGreedyOptionals();

        ProviderNameChecker providerNameChecker = new ProviderNameChecker(getConfigurations());
        providerNameChecker.setRepoURLToTest(repoToTest);
        providerNamesFailure = providerNameChecker.testProviderNames();

        FeatureDisplayableDataChecker licenseChecker = new FeatureDisplayableDataChecker(getConfigurations());
        licenseChecker.setRepoURLToTest(repoToTest);
        licenseConsistencyFailure = licenseChecker.testDisplayableData();

        FeatureNameLengths featureNameLengths = new FeatureNameLengths(getConfigurations());
        featureNameLengths.setRepoURLToTest(repoToTest);
        featureNameLengths.testFeatureDirectoryLength();

        IUVersionCheckToReference iuVersioncheck = new IUVersionCheckToReference(getConfigurations());

        iuVersioncheck.setRepoURLToTest(repoToTest);
        iuVersioncheck.setRepoURLForReference(referenceRepoToTest);

        if (referenceRepoToTest != null) {
            iuVersioncheck.checkIUVersionsToReference();
            iuVersioncheck.checkIUVersionsToReferenceForFeatures();
        }

        if (featureNameFailures || bundleNameFailures || providerNamesFailure || licenseConsistencyFailure || greedyCheck) {
            setFailuresOccurred(true);
        }
    }

    public String getDirectoryToCheck() {
        if (directoryToCheck == null) {
            directoryToCheck = configurations.getReportRepoDir();
        }
        return directoryToCheck;
    }

    public void setDirectoryToCheck(String bundleDirToCheck) {
        this.directoryToCheck = bundleDirToCheck;
    }

    /**
     * Some use of temp working directory, possibly, in theory, can get pretty
     * hefty for large repos/jars, so usually suggested that the standard Java
     * system property java.io.tmpdir be set to something appropriate. (The
     * property defaults to /tmp which on some systems can be fairly small).
     *
     * @return name of directory to use for temporary files
     */
    protected String getTempWorkingDir() {
        if (tempWorkingDir == null) {
            tempWorkingDir = configurations.getTempWorkingDir();
        }
        return tempWorkingDir;
    }

    /**
     * We do allow explicit "setting", but if not set, uses the typical java
     * system property java.io.tmpdir
     *
     * @param tempWorkingDir
     */
    public void setTempWorkingDir(String tempWorkingDir) {
        this.tempWorkingDir = tempWorkingDir;
    }

    public boolean isFailuresOccurred() {
        return failuresOccurred;
    }

    public void setFailuresOccurred(boolean failuresOccurred) {
        // disable, for now
        // this.failuresOccurred = failuresOccurred;
    }

    public String getDirectoryToCheckForReference() {
        if (referenceDirectoryToCheck == null) {
            referenceDirectoryToCheck = configurations.getReferenceRepoDir();
        }
        return referenceDirectoryToCheck;
    }

    public void setDirectoryToCheckForReference(String referenceDirectoryToCheck) {
        this.referenceDirectoryToCheck = referenceDirectoryToCheck;
    }

    public boolean copyTemplateForIndexFile(String filename) throws IOException {
        boolean success = true;
        File topdir = null;
        File indexfile = null;
        InputStream instream = null;
        BufferedInputStream inputstream = null;
        FileWriter indexfileoutput = null;
        try {
            // we'll assume, for now, directories for output has been created.
            topdir = new File(getTopReportOutputDirectory());
            indexfile = new File(topdir, "index.html");
            indexfileoutput = new FileWriter(indexfile);
            instream = this.getClass().getResourceAsStream(filename);
            if (instream != null) {
                inputstream = new BufferedInputStream(instream);
                while (inputstream.available() > 0) {
                    indexfileoutput.write(inputstream.read());
                }
            } else {
                System.out.println("Program Error: did not find expected resource on classpath: " + filename);
            }
        } finally {
            if (indexfileoutput != null) {
                indexfileoutput.close();
            }
            if (instream != null) {
                instream.close();
            }
            if (inputstream != null) {
                inputstream.close();
            }
        }

        return success;
    }

    protected ReportWriter createReportWriter(String outfilename) {
        if (reportWriter == null) {
            String outdir = getReportOutputDirectory();
            reportWriter = new ReportWriter(outdir + "/" + outfilename);
        }
        return reportWriter;
    }

    protected ReportWriter getReportWriter() {
        if (reportWriter == null) {
            handleFatalError("Program Error: getWriter() was called before report writer was created with filename");
        }
        return reportWriter;
    }

    /**
     * @return the configurations
     */
    public RepoTestsConfiguration getConfigurations() {
        return configurations;
    }

}
