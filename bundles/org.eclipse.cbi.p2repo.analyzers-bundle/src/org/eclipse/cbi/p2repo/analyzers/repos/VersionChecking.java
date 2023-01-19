package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;

public class VersionChecking extends TestRepo {

    public VersionChecking(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private static final String EQUALS                 = "equals";
    private static final String STARTS_WITH            = "startsWith";
    private static final int    STARTWITH_MATCH_LENGTH = 5;

    public boolean testVersionUniqness() throws URISyntaxException, ProvisionException, OperationCanceledException, IOException {
        IQueryResult<IInstallableUnit> allIUs = getAllIUs();
        if (allIUs == null) {
            return true;
        }
        analyzeVersionPatterns(allIUs);
        return analyzeNonUniqueVersions(allIUs);
    }

    private void analyzeVersionPatterns(IQueryResult<IInstallableUnit> allIUs) throws IOException {

        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName + "/" + "versionPatterns.txt");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());

            outfileWriter.write("Version qualifer patterns used in repository" + EOL + EOL);
            outfileWriter.write("Repository ('repoURLToTest'): " + getRepoURLToTest() + EOL + EOL);
            outfileWriter.write("(Exploratory report ... maybe eventually used to find typos, such as 'qualfer')" + EOL + EOL);

            List<String> allQualifiers = collectQualifiers(allIUs, outfileWriter);

            collapseSortedQualifiers(outfileWriter, allQualifiers);

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

    private void collapseSortedQualifiers(FileWriter outfileWriter, List<String> allQualifiers) throws IOException, Error {
        String reference = null;
        int refCount = 0;
        String type = EQUALS;
        Collections.sort(allQualifiers);

        outfileWriter.write(EOL + "Count of (sorted) patterns matching up to " + STARTWITH_MATCH_LENGTH + " characters." + EOL
                + EOL);

        for (String q : allQualifiers) {
            if (reference == null) {
                reference = q;
                refCount = 1;
            } else if (reference.equals(q)) {
                refCount++;
                type = EQUALS;
            } else if (reference.isEmpty()) {
                reference = "[empty string]";
                outfileWriter.write(refCount + "\t" + type + "\t\t\t" + reference + EOL);
                reference = q;
                refCount = 1;
                type = EQUALS;
            } else if (startsSimilar(reference, q)) {
                refCount++;
                type = STARTS_WITH;
            } else {
                if (STARTS_WITH.equals(type)) {
                    outfileWriter.write(refCount + "\t" + type + "\t\t" + subsection(reference) + EOL);
                } else if (EQUALS.equals(type)) {
                    outfileWriter.write(refCount + "\t" + type + "\t\t\t" + reference + EOL);
                } else {
                    throw new Error();
                }
                reference = q;
                refCount = 1;
                type = EQUALS;
            }

        }
    }

    private List<String> collectQualifiers(IQueryResult<IInstallableUnit> allIUs, FileWriter outfileWriter) throws IOException,
            Error {
        List<String> allQualifiers = new ArrayList<>();
        int nonOSGiCompatible = 0;
        int nLessThanFour = 0;
        int nMoreThanFour = 0;
        for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {

            // exclude categories, since they are (often) intended to work with
            // same ID, multiple versions
            boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));

            // we exclude these things in some other repo reports ... to improve
            // focus ... but suspect not needed here.
            // boolean isFragment =
            // "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));
            // !isFragment && !isFeatureGroup(iu)
            if (!isCategory && !isSpecial(iu)) {
                // String bundleId = iu.getId();
                Version bundleVersion = iu.getVersion();
                if (bundleVersion.isOSGiCompatible()) {
                    if (bundleVersion.getSegmentCount() == 4) {
                        Comparable<?> qualifier = bundleVersion.getSegment(3);
                        if (qualifier instanceof String qString) {
                            if (qString.isEmpty()) {
                                outfileWriter.write("zero length 4th segment: " + iu.getId() + EOL);
                            }
                            allQualifiers.add(qString);
                        } else {
                            throw new Error("Program Error");
                        }
                    } else if (bundleVersion.getSegmentCount() < 4) {
                        nLessThanFour++;
                    } else if (bundleVersion.getSegmentCount() > 4) {
                        nMoreThanFour++;
                    }
                } else {
                    nonOSGiCompatible++;
                }
            }

        }

        outfileWriter.write(EOL + "Number of nonOSGi compatible versions: " + nonOSGiCompatible + EOL);
        outfileWriter.write(EOL + "Number with more than 4 segments: " + nMoreThanFour + EOL);
        outfileWriter.write(EOL + "Number wtih less than 4 segments: " + nLessThanFour + EOL + EOL);
        return allQualifiers;
    }

    private boolean startsSimilar(String reference, String q) {
        return (q.startsWith(subsection(reference)));
    }

    private String subsection(String reference) {
        // length = endOffset - startOffset;

        String result = reference;
        if (reference.length() >= STARTWITH_MATCH_LENGTH) {
            result = (reference.substring(0, STARTWITH_MATCH_LENGTH));
        }
        return result;

    }

    private boolean analyzeNonUniqueVersions(IQueryResult<IInstallableUnit> allIUs) throws IOException {
        Map<String, Set<Version>> bundles = tabulateNonUniqueIDs(allIUs);
        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "nonUniqueVersions.txt");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());

            outfileWriter.write("Non Unique Versions used in repository" + EOL + EOL);
            outfileWriter.write("Repository ('repoURLToTest'): " + getRepoURLToTest() + EOL + EOL);

            int nUnique = 0;
            for (Map.Entry<String, Set<Version>> entry : bundles.entrySet()) {
                Set<Version> versionSet = entry.getValue();
                if (versionSet.size() == 1) {
                    nUnique++;
                } else {
                    printId(outfileWriter, entry.getKey());
                    for (Version version : versionSet) {
                        printVersion(outfileWriter, version);
                    }
                }
            }

            outfileWriter.write(EOL + "Number of unique id-versions " + nUnique + EOL);

            // always return false (no error), for now ... eventually may be
            // able to
            // figure out some heuristic to return "error", such as if new,
            // non-expected multiple versions
            return false;
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

    private Map<String, Set<Version>> tabulateNonUniqueIDs(IQueryResult<IInstallableUnit> allIUs) {
        Map<String, Set<Version>> bundles = new HashMap<>();
        for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {
            try {
                // exclude categories, since they are (often) intended to work
                // with same ID, multiple versions
                boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));

                // we exclude these things in some other repo reports ... to
                // improve focus ... but suspect not needed here.
                // boolean isFragment =
                // "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));
                // && !isSpecial(iu) && !isFragment && !isFeatureGroup(iu)
                if (!isCategory) {
                    String bundleId = iu.getId();
                    Version bundleVersion = iu.getVersion();
                    // if bundle ID is already in map, we must have already
                    // found at least one
                    if (bundles.containsKey(bundleId)) {
                        Set<Version> versionSet = bundles.get(bundleId);
                        versionSet.add(bundleVersion);
                    } else {
                        // haven't found one yet, so add it, with initial set of
                        // its (one) version
                        Set<Version> versionSet = new HashSet<>();
                        versionSet.add(bundleVersion);
                        bundles.put(bundleId, versionSet);
                    }

                }

            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bundles;
    }

    private void printId(FileWriter out, Object id) throws IOException {
        out.write(id + EOL);
    }

    private void printVersion(FileWriter out, Object version) throws IOException {
        out.write("\t" + version + EOL);
    }

}
