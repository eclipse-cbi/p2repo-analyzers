package org.eclipse.simrel.tests.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.ICopyright;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.repos.TestRepo;
import org.eclipse.simrel.tests.utils.IUIdComparator;

public class FeatureDisplayableDataChecker extends TestRepo {
    public FeatureDisplayableDataChecker(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private static String STANDARD_LICENSES_PROPERTIES_FILE = "standardLicenses.properties";
    private String        previousPackageHeader             = null;

    public static void main(String[] args) {
        _testFeatureDisplayableDataChecer();

        _testGetInitialSegments();
    }

    private static void _testFeatureDisplayableDataChecer() {
        System.out.println("\n\n\tLocal test of  FeatureDisplayableDataChecker.\n\n");
        FeatureDisplayableDataChecker featureDisplayableDataChecker = new FeatureDisplayableDataChecker(RepoTestsConfiguration.createFromSystemProperties());
        featureDisplayableDataChecker.setRepoURLToTest("file:///home/shared/simrel/luna/aggregation/final");
        featureDisplayableDataChecker.setMainOutputDirectory("/home/shared/simrel/luna");
        try {
            featureDisplayableDataChecker.testDisplayableData();
        } catch (ProvisionException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void _testGetInitialSegments() {
        System.out.println("\n\n\tTesting getInitialSegments method.\n\n");
        FeatureDisplayableDataChecker instance = new FeatureDisplayableDataChecker(RepoTestsConfiguration.createFromSystemProperties());
        System.out.println(instance.getInitialSegments(""));
        System.out.println(instance.getInitialSegments("one"));
        System.out.println(instance.getInitialSegments("one.two"));
        System.out.println(instance.getInitialSegments("one.two.three"));
        System.out.println(instance.getInitialSegments("one.two.three.four"));
        System.out.println(instance.getInitialSegments("one.two.three.four.five"));
        System.out.println(instance.getInitialSegments("."));
        System.out.println(instance.getInitialSegments("one.two."));
    }

    private boolean checkLicenseConsistency(IQueryResult<IInstallableUnit> allFeatures) throws IOException {
        boolean result = false;
        Properties properties = new Properties();

        InputStream inStream = this.getClass().getResourceAsStream(STANDARD_LICENSES_PROPERTIES_FILE);
        properties.load(inStream);
        String body2014 = properties.getProperty("license2014");
        String body2011 = properties.getProperty("license2011");
        String body2010 = properties.getProperty("license2010");
        ILicense standardLicense2014 = new License(null, body2014, null);
        ILicense standardLicense2011 = new License(null, body2011, null);
        ILicense standardLicense2010 = new License(null, body2010, null);

        List<IInstallableUnit> noLicense = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> extraLicense = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> license2014 = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> license2011 = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> license2010 = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> badLicense = new ArrayList<IInstallableUnit>();
        checkLicenses(standardLicense2014, standardLicense2011, standardLicense2010, allFeatures, license2014, license2011,
                license2010, badLicense, noLicense, extraLicense);

        printReportLicense(license2014, license2011, license2010, badLicense, noLicense, extraLicense);
        result = ((badLicense.size() > 0) || (extraLicense.size() > 0) || (noLicense.size() > 0));
        return result;
    }

    private void checkLicenses(ILicense platformLicense2014, ILicense platformLicense2011, ILicense platformLicense2010,
            IQueryResult<IInstallableUnit> allFeatures, List<IInstallableUnit> license2014, List<IInstallableUnit> license2011,
            List<IInstallableUnit> license2010, List<IInstallableUnit> badLicense, List<IInstallableUnit> noLicense,
            List<IInstallableUnit> extraLicense) {
        System.out.println("Number of IUs during license check: " + allFeatures.toUnmodifiableSet().size());
        int nFeatures = 0;
        for (IInstallableUnit feature : allFeatures.toUnmodifiableSet()) {
            nFeatures++;
            Collection<ILicense> licenses = feature.getLicenses(null);
            if (licenses.isEmpty()) {
                noLicense.add(feature);
                continue;
            }
            if (licenses.size() != 1) {
                extraLicense.add(feature);
                continue;
            }
            ILicense featureLicense = licenses.iterator().next();
            if (platformLicense2010.getUUID().equals(featureLicense.getUUID())) {
                license2010.add(feature);
                continue;
            }
            if (platformLicense2011.getUUID().equals(featureLicense.getUUID())) {
                license2011.add(feature);
                continue;
            }
            if (platformLicense2014.getUUID().equals(featureLicense.getUUID())) {
                license2014.add(feature);
                continue;
            }
            // if we get here, we have some kind of bad license, or its
            // missing.
            String featureLicenseText = featureLicense.getBody();
            if (featureLicenseText == null || featureLicenseText.length() == 0) {
                noLicense.add(feature);
            }
            // "bad" in this context means different from one of the
            // standard ones.
            badLicense.add(feature);

        }
        System.out.println("Number features or products during license check: " + nFeatures);

    }

    private void printReportLicense(List<IInstallableUnit> license2014, List<IInstallableUnit> license2011,
            List<IInstallableUnit> license2010, List<IInstallableUnit> badLicense, List<IInstallableUnit> noLicense,
            List<IInstallableUnit> extraLicense) {

        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "licenseConsistency.html");
            outfileWriter = new FileWriter(outfile);
            printparagraph(outfileWriter, "Repository ('repoURLToTest'): " + getRepoURLToTest());
            printparagraph(outfileWriter, "\toutput: " + outfile.getAbsolutePath());
            printHeader(outfileWriter, 2, "License Consistency Summary");
            println(outfileWriter, "Features with conforming (2014) license: " + license2014.size());
            println(outfileWriter, "Features with old (2011) license: " + license2011.size());
            println(outfileWriter, "Features with old (2010) license: " + license2010.size());
            println(outfileWriter, "Features with different (or no) license: " + badLicense.size());
            println(outfileWriter, "Features with no license attribute: " + noLicense.size());
            println(outfileWriter, "Features with extra licenses: " + extraLicense.size());

            printHeader(outfileWriter, 2, "Details");

            printHeader(outfileWriter, 3, "Features with no license attribute");
            Collections.sort(noLicense, new IUIdComparator());
            for (IInstallableUnit unit : noLicense) {
                println(outfileWriter, printableIdString(unit));
            }

            printHeader(outfileWriter, 3, "Features with different (or no) license (and first few lines of license text)");
            Collections.sort(badLicense, new IUIdComparator());
            for (IInstallableUnit unit : badLicense) {
                makeHeaderIfNeeded(outfileWriter, unit.getId());
                println(outfileWriter, printableIdString(unit));
                Collection<ILicense> licenses = unit.getLicenses();
                ILicense featureLicense = licenses.iterator().next();
                String licenseText = featureLicense.getBody();
                // by being in "bad feature list" there should always be at
                // least
                // some text, as checked in checkLicenseConsistency method.
                int amount = Math.min(200, licenseText.length());
                String shortLicenseText = licenseText.substring(0, amount);
                printparagraph(outfileWriter, shortLicenseText);
            }
            printHeader(outfileWriter, 3, "Features with old (2010) license");
            Collections.sort(license2010, new IUIdComparator());
            for (IInstallableUnit unit : license2010) {
                println(outfileWriter, printableIdString(unit));
            }
            printHeader(outfileWriter, 3, "Features with old (2011) license");
            Collections.sort(license2011, new IUIdComparator());
            for (IInstallableUnit unit : license2011) {
                println(outfileWriter, printableIdString(unit));
            }
            printHeader(outfileWriter, 3, "Features with current (2014) license");
            Collections.sort(license2014, new IUIdComparator());
            for (IInstallableUnit unit : license2014) {
                println(outfileWriter, printableIdString(unit));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outfileWriter != null) {
                try {
                    outfileWriter.close();
                } catch (IOException e) {
                    // weirdness
                    e.printStackTrace();
                }
            }
        }
        // if ((badLicense.size() > 0) || (extraLicense.size() > 0) ||
        // (noLicense.size() > 0)) {
        // fail("Errors in license consistency. For list, see " +
        // outfile.getAbsolutePath());
        // }
    }

    private String printableIdString(IInstallableUnit unit) {
        String printIdString = unit.getId();
        // we want to mark "products" in a special way, since not sure where
        // they are displayed.
        String productString = unit.getProperty("org.eclipse.equinox.p2.type.product");
        boolean productValue = "true".equals(productString);
        if (productValue) {
            printIdString += " [Product]";
        }
        return printIdString;
    }

    private void makeHeaderIfNeeded(FileWriter outputfile, String id) throws IOException {

        String proposedHeader = getInitialSegments(id);
        if (proposedHeader == null || proposedHeader.length() == 0) {
            proposedHeader = "Unexpectedly empty package name?";
        }
        if (previousPackageHeader == null || !previousPackageHeader.equals(proposedHeader)) {
            previousPackageHeader = proposedHeader;
            printHeader(outputfile, 4, proposedHeader);
        }
    }

    private String getInitialSegments(String id) {
        String proposedHeader = "";
        if (!(id == null || id.length() == 0)) {
            String[] proposedHeaderArray = id.split("\\.");
            if (proposedHeaderArray.length > 0) {
                int nSegments = Math.min(proposedHeaderArray.length, 3);
                int i = 0;
                do {
                    if (proposedHeader.length() != 0) {
                        proposedHeader = proposedHeader + ".";
                    }
                    proposedHeader = proposedHeader + proposedHeaderArray[i];
                    i++;
                } while (i < nSegments);
            }
        }
        return proposedHeader;
    }

    public boolean testDisplayableData() throws URISyntaxException, ProvisionException, OperationCanceledException, IOException {
        boolean result = false;
        IQueryResult<IInstallableUnit> allIUs = getAllGroupIUs();
        result = checkLicenseConsistency(allIUs);
        result = result | checkCopyrights(allIUs);
        result = result | checkDescriptions(allIUs);
        return result;
    }

    private boolean checkCopyrights(IQueryResult<IInstallableUnit> allFeatures) {
        boolean result = false;

        List<IInstallableUnit> noOrBadCopyright = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> okCopyright = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> indeterminateCopyright = new ArrayList<IInstallableUnit>();

        for (IInstallableUnit feature : allFeatures.toUnmodifiableSet()) {
            if (feature.getId().endsWith(".feature.group")) {
                ICopyright copyright = feature.getCopyright(null);
                if (copyright == null) {
                    noOrBadCopyright.add(feature);
                    continue;
                }
                String body = copyright.getBody();
                if ((body == null) || (body.length() == 0) || body.startsWith("%") || body.startsWith("[")) {
                    noOrBadCopyright.add(feature);
                    continue;
                }

                if (body.startsWith("Copyright") || body.startsWith("(c) Copyright")) {
                    okCopyright.add(feature);
                    continue;
                }
                // the order of these tests clauses matter, obvously. (Compare
                // with above 'startsWith'.)
                if (body.contains("Copyright") || body.contains("copyright")) {
                    indeterminateCopyright.add(feature);
                    continue;
                }
                // fallthrough assumed "bad"
                noOrBadCopyright.add(feature);
            }

        }

        printReportCopyrights(okCopyright, noOrBadCopyright, indeterminateCopyright);
        result = ((noOrBadCopyright.size() > 0));
        return result;
    }

    private boolean checkDescriptions(IQueryResult<IInstallableUnit> allFeatures) {
        boolean result = false;

        List<IInstallableUnit> noneOrBad = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> ok = new ArrayList<IInstallableUnit>();

        for (IInstallableUnit feature : allFeatures.toUnmodifiableSet()) {
            if (feature.getId().endsWith(".feature.group")) {
                String description = feature.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
                if ((description == null) || (description.length() <= 0) || description.startsWith("%")) {
                    noneOrBad.add(feature);
                    continue;
                }

                ok.add(feature);
            }

        }

        printReportDescription(ok, noneOrBad);
        result = ((noneOrBad.size() > 0));
        return result;
    }

    private void printReportDescription(List<IInstallableUnit> ok, List<IInstallableUnit> noneOrBad) {
        String SPACER = "<br />=======================";

        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "descriptions.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());
            println(outfileWriter, "<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            println(outfileWriter, "<br /><br />Description Report:" + SPACER);
            println(outfileWriter, "Features wrong or unexplainable descriptions: " + noneOrBad.size());
            println(outfileWriter, "Features probably correct: " + ok.size());
            println(outfileWriter, "=======================");

            println(outfileWriter, "<br /><br />Details:" + SPACER);

            println(outfileWriter, "Features with no or dubious descripiton:" + SPACER);
            Collections.sort(noneOrBad, new IUIdComparator());
            for (IInstallableUnit unit : noneOrBad) {
                printLineDescription(outfileWriter, unit);
            }

            println(outfileWriter, "<br /><br />Features with descripitons:" + SPACER);
            Collections.sort(ok, new IUIdComparator());
            for (IInstallableUnit unit : ok) {
                printLineDescription(outfileWriter, unit);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outfileWriter != null) {
                try {
                    outfileWriter.close();
                } catch (IOException e) {
                    // weirdness
                    e.printStackTrace();
                }
            }
        }
        // if ((badLicense.size() > 0) || (extraLicense.size() > 0) ||
        // (noLicense.size() > 0)) {
        // fail("Errors in license consistency. For list, see " +
        // outfile.getAbsolutePath());
        // }
    }

    private void printReportCopyrights(List<IInstallableUnit> good, List<IInstallableUnit> bad, List<IInstallableUnit> maybe) {
        String SPACER = "<br />=======================";

        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "copyrights.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());
            println(outfileWriter, "<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            println(outfileWriter, "<br /><br />Copyright Report:" + SPACER);
            println(outfileWriter, "Features wrong or unexplainable copyrights: " + bad.size());
            println(outfileWriter, "Features maybe wrong, maybe correct copyrights: " + maybe.size());
            println(outfileWriter, "Features probably correct: " + good.size());
            println(outfileWriter, "=======================");

            println(outfileWriter, "<br /><br />Details:" + SPACER);

            println(outfileWriter, "Features with no or probably incorrect copyright statement:" + SPACER);
            Collections.sort(bad, new IUIdComparator());
            for (IInstallableUnit unit : bad) {
                printLineCopyright(outfileWriter, unit);
            }

            println(outfileWriter,
                    "<br /><br />Indeterminant: feature's copyright text contains the word 'copyright' but not at beginning:"
                            + SPACER);
            Collections.sort(maybe, new IUIdComparator());
            for (IInstallableUnit unit : maybe) {
                printLineCopyright(outfileWriter, unit);
            }
            println(outfileWriter, "<br /><br />Features with copyrights that are probably ok (i.e. start with 'Copyright'):"
                    + SPACER);
            Collections.sort(good, new IUIdComparator());
            for (IInstallableUnit unit : good) {
                printLineCopyright(outfileWriter, unit);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outfileWriter != null) {
                try {
                    outfileWriter.close();
                } catch (IOException e) {
                    // weirdness
                    e.printStackTrace();
                }
            }
        }
        // if ((badLicense.size() > 0) || (extraLicense.size() > 0) ||
        // (noLicense.size() > 0)) {
        // fail("Errors in license consistency. For list, see " +
        // outfile.getAbsolutePath());
        // }
    }

    private void printLineCopyright(FileWriter outfileWriter, IInstallableUnit iu) throws IOException {

        String copyright = null;
        ICopyright copyrightIu = iu.getCopyright(null);
        if (copyrightIu != null) {
            copyright = copyrightIu.getBody();
        }
        String iuId = iu.getId();
        String iuVersion = iu.getVersion().toString();
        println(outfileWriter, iuId + NBSP + iuVersion + BR + NBSP + copyright);
    }

    private void printLineDescription(FileWriter outfileWriter, IInstallableUnit iu) throws IOException {

        String description = iu.getProperty(IInstallableUnit.PROP_DESCRIPTION, null);
        String iuId = iu.getId();
        String iuVersion = iu.getVersion().toString();
        println(outfileWriter, iuId + NBSP + iuVersion + BR + NBSP + description);
    }
}
