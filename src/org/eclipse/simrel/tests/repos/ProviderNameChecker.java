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
import org.eclipse.simrel.tests.repos.TestRepo;
import org.eclipse.simrel.tests.utils.IUIdComparator;

public class ProviderNameChecker extends TestRepo {
    private static final String OLD_PROVIDER_NAME       = "Eclipse.org";
    private String[]            EXPECTED_PROVIDER_NAMES = { "Eclipse Equinox Project", "Eclipse PTP", "Eclipse Orbit",
            "Eclipse Web Tools Platform", "Eclipse CDT", "Eclipse Agent Modeling Platform", "Eclipse BIRT Project",
            "Eclipse Data Tools Platform", "Eclipse Modeling Project", "Eclipse Mylyn", "Eclipse Memory Analyzer",
            "Eclipse Linux Tools", "Eclipse Jubula", "Eclipse Jetty Project", "Eclipse Gyrex", "Eclipse EGit", "Eclipse JGit",
            "Eclipse Agent Modeling Project", "Eclipse Packaging Project", "Eclipse Scout Project", "Eclipse Sequoyah",
            "Eclipse TM Project", "Eclipse SOA", "Eclipse Koneki", "Eclipse Model Focusing Tools", "Eclipse Code Recommenders",
            "Eclipse RTP", "Eclipse Stardust", "Eclipse JWT", "Eclipse Xtend" };

    private boolean checkProviderNames(IQueryResult<IInstallableUnit> allIUs) throws IOException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> incorrectProviderName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> correctProviderName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> oldProviderName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> unknownProviderName = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> suspectProviderName = new ArrayList<IInstallableUnit>();
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "providerNames.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());
            for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {
                try {
                    // ignore categories
                    boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));
                    // TODO: should we exclude fragments?
                    boolean isFragment = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));
                    boolean isProduct = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.product"));

                    // || iu.getId().endsWith("feature.group")
                    if (!isCategory && !isSpecial(iu) && !isFragment && !isProduct) {
                        String providerName = iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
                        if (providerName == null) {
                            incorrectProviderName.add(iu);
                        }
                        // common errors and misspellings
                        else if (providerName.startsWith("%") || providerName.equals("Eclipse")
                                || providerName.startsWith("eclipse.org") 
                                || providerName.equals("unknown")
                                || providerName.startsWith("Engineering") || providerName.contains("org.eclipse.jwt")
                                || providerName.contains("www.example.org") || providerName.contains("www.eclipse.org")
                                || providerName.contains("Provider") || providerName.contains("provider")
                                || providerName.startsWith("Bundle-") || providerName.startsWith("bund")
                                || providerName.startsWith("Eclispe")) {
                            incorrectProviderName.add(iu);
                        } else if (providerName.startsWith("Eclipse.org - ")) {
                            correctProviderName.add(iu);
                        } else if (inListOfExpectedName(providerName)) {
                            correctProviderName.add(iu);
                        } else if (OLD_PROVIDER_NAME.equals(providerName)) {
                            oldProviderName.add(iu);
                        }
                        // order is important, starts with Eclipse, but not one
                        // of the above e.g. "Eclipse Orbit" or "Eclipse.org"?
                        // TODO: eventually put in with "incorrect?"
                        else if (providerName.startsWith("Eclipse")) {
                            unknownProviderName.add(iu);
                        } else {
                            if (iu.getId().startsWith("org.eclipse")) {
                                suspectProviderName.add(iu);
                            } else {
                                unknownProviderName.add(iu);
                            }
                        }
                        // experiment to find configs and categories
                        if (DEBUG) {
                            if (providerName == null) {
                                printAllProperties(outfileWriter, iu);
                            }
                        }
                    }

                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }

            outfileWriter.write("<h1>Provider names used in repository</h1>" + EOL);
            outfileWriter.write("<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            outfileWriter.write("<h2>Major: Suspect or (probably) incorrect provider name</h2>" + EOL);
            printLinesProvider(outfileWriter, suspectProviderName);
            outfileWriter.write("<h2>Major: missing or (probably) incorrect provider name</h2>" + EOL);
            printLinesProvider(outfileWriter, incorrectProviderName);
            outfileWriter.write("<h2>Indeterminate: maybe correct, maybe incorrect provider name</h2>" + EOL);
            printLinesProvider(outfileWriter, unknownProviderName);
            outfileWriter.write("<h2>Old style provider name</h2>" + EOL);
            printLinesProvider(outfileWriter, oldProviderName);
            outfileWriter.write("<h2>Probably using correctly branding provider name</h2>" + EOL);
            printLinesProvider(outfileWriter, correctProviderName);
            outfileWriter.write("<h2>List of known branding provider names</h2>" + EOL);
            for (int i = 0; i < EXPECTED_PROVIDER_NAMES.length; i++) {
                println(outfileWriter, EXPECTED_PROVIDER_NAMES[i] + EOL);
            }

            // if (incorrectProviderName.size() > 0) {
            // fail("Errors in naming or localization. For list, see " +
            // outfile.getName());
            // }
            return incorrectProviderName.size() > 0;
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

    public boolean testProviderNames() throws URISyntaxException, ProvisionException, OperationCanceledException, IOException {
        IQueryResult<IInstallableUnit> allIUs = getAllIUs();
        return checkProviderNames(allIUs);
    }

    private boolean inListOfExpectedName(String providerName) {
        boolean result = false;
        for (int i = 0; i < EXPECTED_PROVIDER_NAMES.length; i++) {
            if (EXPECTED_PROVIDER_NAMES[i].equals(providerName)) {
                result = true;
                break;
            }
        }
        return result;

    }

    private void printLinesProvider(FileWriter out, List<IInstallableUnit> iuList) throws IOException {
        // Comparator<? super IInstallableUnit> comparatorProviderName = new
        // IUProviderAndIdComparator();
        Comparator<? super IInstallableUnit> comparatorProviderName = new IUIdComparator();
        Collections.sort(iuList, comparatorProviderName);
        out.write("<p>Count: " + iuList.size() + EOL);
        out.write("<ol>" + EOL);

        for (IInstallableUnit iu : iuList) {
            printLineListItem(out, iu, IInstallableUnit.PROP_PROVIDER);
        }
        out.write("</ol>" + EOL);
    }
}
