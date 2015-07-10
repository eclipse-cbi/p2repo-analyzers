package org.eclipse.simrel.tests.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.utils.IUIdComparator;

public class ProviderNameChecker extends TestRepo {
    public ProviderNameChecker(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private static final String OLD_PROVIDER_NAME           = "Eclipse.org";
    private static final String KNOWN_PROVIDERS_RESOURCE    = "knownProviders.properties";
    private static final String EXPECTED_PROVIDER_NAMES_KEY = "expectedProviderNames";
    private ArrayList<String>   expectedProvidersName       = null;

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
                    if (!isCategory && !isSpecial(iu) && !isFragment && !isProduct && !isEclipseLicenseIU(iu)) {
                        String providerName = iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
                        if (providerName == null) {
                            incorrectProviderName.add(iu);
                        }
                        // common errors and misspellings
                        else if (providerName.startsWith("%") || providerName.equals("Eclipse")
                                || providerName.startsWith("eclipse.org") || providerName.equals("unknown")
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
            ArrayList<String> expectedProvidersNameLocal = getKnownProviderNames();
            for (int i = 0; i < expectedProvidersNameLocal.size(); i++) {
                println(outfileWriter, expectedProvidersNameLocal.get(i) + EOL);
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
        ArrayList<String> expectedProvidersNameLocal = getKnownProviderNames();
        boolean result = false;
        if (expectedProvidersNameLocal != null) {
            for (int i = 0; i < expectedProvidersNameLocal.size(); i++) {
                if (expectedProvidersNameLocal.get(i).equals(providerName)) {
                    result = true;
                    break;
                }
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

    protected ArrayList<String> getKnownProviderNames() {
        if (expectedProvidersName == null) {
            ArrayList<String> namesAsList = new ArrayList<String>();
            // first try system properties, to allow override.
            String expectedProviders = System.getProperty(EXPECTED_PROVIDER_NAMES_KEY);
            if (expectedProviders == null) {
                // if no system property found, use out built-in list
                Properties names = new Properties();
                InputStream inStream = null;
                try {
                    inStream = getClass().getResourceAsStream(KNOWN_PROVIDERS_RESOURCE);
                    try {
                        names.load(inStream);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    expectedProviders = names.getProperty(EXPECTED_PROVIDER_NAMES_KEY);
                    if (expectedProviders == null) {
                        throw new Error("PROGRAM ERROR: Could not read internal property file");
                    }
                    StringTokenizer tokenizer = new StringTokenizer(expectedProviders, ",", false);
                    while (tokenizer.hasMoreTokens()) {
                        String name = tokenizer.nextToken();
                        namesAsList.add(name);
                    }
                } finally {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                expectedProvidersName = namesAsList;
            }
        }
        return expectedProvidersName;
    }
}
