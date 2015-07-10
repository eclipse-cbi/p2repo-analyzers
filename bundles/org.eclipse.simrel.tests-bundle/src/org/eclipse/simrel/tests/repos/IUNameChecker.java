package org.eclipse.simrel.tests.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.repos.TestRepo;
import org.eclipse.simrel.tests.utils.IUIdComparator;

public class IUNameChecker extends TestRepo {

    public IUNameChecker(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    public boolean testBundleNames() throws URISyntaxException, ProvisionException, OperationCanceledException, IOException {
        IQueryResult<IInstallableUnit> allIUs = getAllIUs();
        return checkBundleNames(allIUs);
    }

    public boolean testFeatureNames() throws URISyntaxException, ProvisionException, OperationCanceledException, IOException {
        IQueryResult<IInstallableUnit> allIUs = getAllGroupIUs();
        return checkFeatureNames(allIUs);
    }

    private boolean checkFeatureNames(IQueryResult<IInstallableUnit> allIUs) throws IOException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> incorrectBundleName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> probablyCorrectBundleName = new ArrayList<IInstallableUnit>();
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "featureNames.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());
            for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {
                try {
                    // ignore categories
                    boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));
                    // TODO: should we exclude fragments?
                    boolean isFragment = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));

                    // TODO: probably do not need to exclude all of these? Since
                    // we are specifically selecting "groups"?
                    if ((isGroup(iu) && !isCategory) && !isSpecial(iu) && !isFragment && !isEclipseLicenseIU(iu)) {
                        String bundleName = iu.getProperty(IInstallableUnit.PROP_NAME, null);
                        // not sure fi can ever be null ... but, just in case
                        if (bundleName == null) {
                            incorrectBundleName.add(iu);
                        } else if ((bundleName.startsWith("%") || bundleName.startsWith("Feature-")
                                || bundleName.startsWith("Bundle-") || bundleName.startsWith("feature")
                                || bundleName.startsWith("plugin") || bundleName.startsWith("Plugin.name")
                                || bundleName.startsWith("fragment.") || bundleName.startsWith("Eclipse.org") || bundleName
                                    .startsWith("bundle"))) {
                            incorrectBundleName.add(iu);
                        } else {
                            probablyCorrectBundleName.add(iu);
                        }
                        // experiment to find configs and categories
                        if (DEBUG) {
                            if (bundleName == null) {
                                printAllProperties(outfileWriter, iu);
                            }
                        }
                    }

                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            outfileWriter.write("<h1>Feature names used in repository</h1>" + EOL);
            outfileWriter.write("<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            outfileWriter.write("<h2>Major: missing or (probably) incorrect names</h2>" + EOL);
            printLinesBundleName(outfileWriter, incorrectBundleName);
            outfileWriter.write("<h2>Probably correct names</h2>" + EOL);
            printLinesBundleName(outfileWriter, probablyCorrectBundleName);

            // if (incorrectBundleName.size() > 0) {
            // fail("Errors in naming or localization. For list, see " +
            // outfile.getName());
            // }
            return incorrectBundleName.size() > 0;
        } finally {
            if (outfileWriter != null) {
                try {
                    outfileWriter.close();
                } catch (IOException e) {
                    // would be weird
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean checkBundleNames(IQueryResult<IInstallableUnit> allIUs) throws IOException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> incorrectBundleName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> probablyCorrectBundleName = new ArrayList<IInstallableUnit>();
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "bundleNames.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());
            for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {
                try {
                    // ignore categories
                    boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));
                    // TODO: should we exclude fragments?
                    boolean isFragment = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));

                    // we exclude feature groups here, so they can be in their
                    // own list, for better focus
                    if (!isCategory && !isSpecial(iu) && !isFragment && !isGroup(iu) &&!isEclipseLicenseIU(iu)) {
                        String bundleName = iu.getProperty(IInstallableUnit.PROP_NAME, null);
                        // not sure fi can ever be null ... but, just in case
                        if (bundleName == null) {
                            incorrectBundleName.add(iu);
                        } else if ((bundleName.startsWith("%") || bundleName.startsWith("Bundle-")
                                || bundleName.startsWith("plugin") || bundleName.startsWith("Plugin.name")
                                || bundleName.startsWith("fragment.") || bundleName.startsWith("Eclipse.org") || bundleName
                                    .startsWith("bundle"))) {
                            incorrectBundleName.add(iu);
                        } else {
                            probablyCorrectBundleName.add(iu);
                        }
                        // experiment to find configs and categories
                        if (DEBUG) {
                            if (bundleName == null) {
                                printAllProperties(outfileWriter, iu);
                            }
                        }
                    }

                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            outfileWriter.write("<h1>Bundle names used in repository</h1>" + EOL);
            outfileWriter.write("<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            // outfileWriter.write("<p>Note, there are several problems with this test, e.g. see <a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id=309566\">bug 309566</a>. So provided here as FYI for now.</p>"
            // + EOL);
            outfileWriter.write("<h2>Major: missing or (probably) incorrect bundle name</h2>" + EOL);
            printLinesBundleName(outfileWriter, incorrectBundleName);
            outfileWriter.write("<h2>Probably correct bundle name</h2>" + EOL);
            printLinesBundleName(outfileWriter, probablyCorrectBundleName);

            // if (incorrectBundleName.size() > 0) {
            // fail("Errors in naming or localization. For list, see " +
            // outfile.getName());
            // }
            return incorrectBundleName.size() > 0;
        } finally {
            if (outfileWriter != null) {
                try {
                    outfileWriter.close();
                } catch (IOException e) {
                    // would be weird
                    e.printStackTrace();
                }
            }
        }
    }

    private void printLinesBundleName(FileWriter out, List<IInstallableUnit> iuList) throws IOException {
        // Comparator<? super IInstallableUnit> comparatorBundleName = new
        // IUNameAndIdComparator();
        Comparator<? super IInstallableUnit> comparatorBundleName = new IUIdComparator();
        Collections.sort(iuList, comparatorBundleName);
        out.write("<p>Count: " + iuList.size() + EOL);
        out.write("<ol>" + EOL);

        for (IInstallableUnit iu : iuList) {
            printLineListItem(out, iu, IInstallableUnit.PROP_NAME);
        }
        out.write("</ol>" + EOL);
    }

}
