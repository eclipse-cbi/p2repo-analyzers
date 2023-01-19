package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.IUIdComparator;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;

public class IUVersionCheckToReference extends TestRepo {

    public IUVersionCheckToReference(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    public boolean checkIUVersionsToReference() throws IOException, ProvisionException, URISyntaxException {
        String testDirName = getReportOutputDirectory();
        File outfile = new File(testDirName, "versionChecks.html");
        List<IInstallableUnit> referenceOnly = new ArrayList<>();
        List<IInstallableUnit> newIUs = new ArrayList<>();
        List<IInstallableUnit> decreasingVersions = new ArrayList<>();
        List<IInstallableUnit> matchingVersions = new ArrayList<>();
        List<IInstallableUnit> increaseVersionsMajor = new ArrayList<>();
        List<IInstallableUnit> increaseVersionsMinor = new ArrayList<>();
        List<IInstallableUnit> increaseVersionsService = new ArrayList<>();
        List<IInstallableUnit> increaseVersionsQualifierOnly = new ArrayList<>();
        Set<String> refinboth = new TreeSet<>();
        Set<String> curinboth = new TreeSet<>();

        try (FileWriter outfileWriter = new FileWriter(outfile)){
            System.out.println("output: " + outfile.getAbsolutePath());

            outfileWriter.write("<h1>All IUs</h1>" + EOL + "<p>(except groups, and categories)</p>");
            // The "System.out" lines are for sanity check/debugging purposes.
            outfileWriter.write("<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            if (!getRepoURLForReference().isEmpty()) {
                outfileWriter.write("<p>Repository for reference ('repoURLForReference'): " + getRepoURLForReference() + "</p>"
                        + EOL);
            }

            System.out.println("Number in reference from raw count:" + rawcount(getAllReferenceIUs()));
            if (getAllReferenceIUs() != null) {
                System.out.println("Number in reference from set:" + getAllReferenceIUs().toSet().size());
            }
            System.out.println("Number in current from raw count:" + rawcount(getAllIUs()));
            System.out.println("Number in current from set: " + getAllIUs().toSet().size());

            processForExtraReferences(referenceOnly, refinboth);

            System.out.println("Number in reference missing from current: " + referenceOnly.size());
            System.out.println("Number in common in reference and current: " + refinboth.size());
            // dump(refinboth, "inBothRef.txt");

            processForNewBundles(newIUs, curinboth);

            System.out.println("Number in current missing from reference: " + newIUs.size());
            System.out.println("Number in common in reference and current: " + curinboth.size());
            // dump(curinboth, "inBothCurrent.txt");

            outfileWriter.write("<h2>IUs in reference, but not in current repo</h2>" + EOL);
            printLinesIUs(outfileWriter, referenceOnly);

            outfileWriter.write("<h2>IUs in current, but not in reference</h2>" + EOL);
            printLinesIUs(outfileWriter, newIUs);

            processForDifferences(curinboth, getAllIUs(), getAllReferenceIUs(), decreasingVersions, matchingVersions,
                    increaseVersionsMajor, increaseVersionsMinor, increaseVersionsService, increaseVersionsQualifierOnly);

            outfileWriter.write("<h2>IUs in current repo that decrease versions</h2>" + EOL);
            printTableIUs(outfileWriter, decreasingVersions, getAllReferenceIUs());

            outfileWriter.write("<h2>IUs in current repo that increase major versions</h2>" + EOL);
            printTableIUs(outfileWriter, increaseVersionsMajor, getAllReferenceIUs());

            outfileWriter.write("<h2>IUs in current repo that increase minor versions</h2>" + EOL);
            printTableIUs(outfileWriter, increaseVersionsMinor, getAllReferenceIUs());

            outfileWriter.write("<h2>IUs in current repo that increase versions but with qualifier only</h2>" + EOL);
            printTableIUs(outfileWriter, increaseVersionsQualifierOnly, getAllReferenceIUs());

            outfileWriter.write("<h2>IUs in current repo that increase service versions</h2>" + EOL);
            printTableIUs(outfileWriter, increaseVersionsService, getAllReferenceIUs());

            outfileWriter.write("<h2>IUs in current repo that have matching versions in reference repo</h2>" + EOL);
            printTableIUs(outfileWriter, matchingVersions, getAllReferenceIUs());

            return true;
        }

    }

    private void processForDifferences(Set<String> curinboth, IQueryResult<IInstallableUnit> allIUs,
            IQueryResult<IInstallableUnit> allReferenceIUs, List<IInstallableUnit> decreasingVersions,
            List<IInstallableUnit> matchingVersions, List<IInstallableUnit> increaseVersionsMajor,
            List<IInstallableUnit> increaseVersionsMinor, List<IInstallableUnit> increaseVersionsService,
            List<IInstallableUnit> increaseVersionsQualifierOnly) {

        Set<IInstallableUnit> allCurrent = allIUs.toUnmodifiableSet();
        Set<IInstallableUnit> allRef = allReferenceIUs.toUnmodifiableSet();

        for (String iuname : curinboth) {
            IInstallableUnit current = getIU(iuname, allCurrent);
            IInstallableUnit reference = getIU(iuname, allRef);
            if (reference != null) {
                Version curVersion = current.getVersion();
                Version refVersion = reference.getVersion();
                if (curVersion.equals(refVersion)) {
                    matchingVersions.add(current);
                } else {
                    Comparable curMajor = curVersion.getSegment(0);
                    Comparable refMajor = refVersion.getSegment(0);
                    if (curMajor.compareTo(refMajor) < 0) {
                        decreasingVersions.add(current);
                    } else if (curMajor.compareTo(refMajor) > 0) {
                        increaseVersionsMajor.add(current);
                    } else if (curMajor.compareTo(refMajor) == 0) {
                        Comparable curMinor = curVersion.getSegment(1);
                        Comparable refMinor = refVersion.getSegment(1);
                        if (curMinor.compareTo(refMinor) < 0) {
                            decreasingVersions.add(current);
                        } else if (curMinor.compareTo(refMinor) > 0) {
                            increaseVersionsMinor.add(current);
                        } else if (curMinor.compareTo(refMinor) == 0) {
                            Comparable curService = curVersion.getSegment(2);
                            Comparable refService = refVersion.getSegment(2);
                            if (curService.compareTo(refService) < 0) {
                                decreasingVersions.add(current);
                            } else if (curService.compareTo(refService) > 0) {
                                increaseVersionsService.add(current);
                            } else if (curService.compareTo(refService) == 0) {
                                Comparable curQualifier = curVersion.getSegment(3);
                                Comparable refQualifier = refVersion.getSegment(3);
                                if (curQualifier.compareTo(refQualifier) < 0) {
                                    decreasingVersions.add(current);
                                } else if (curQualifier.compareTo(refQualifier) > 0) {
                                    increaseVersionsQualifierOnly.add(current);
                                } else if (curQualifier.compareTo(refQualifier) == 0) {
                                    System.out.print("Surprising we'd get here, since already checked for equality");
                                    matchingVersions.add(current);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private IInstallableUnit getIU(String iuname, Set<IInstallableUnit> allIUs) {
        IInstallableUnit result = null;
        for (IInstallableUnit iu : allIUs) {
            if (iuname.equals(iu.getId())) {
                result = iu;
                break;
            }
        }
        return result;
    }

    private int rawcount(IQueryResult<IInstallableUnit> refs) {
        int count = 0;
        if (refs != null) {
            for (IInstallableUnit iiu : refs) {
                if (iiu.getId() == null || iiu.getId().isEmpty()) {
                    throw new RuntimeException("IIU had no (or empty) ID string: " + iiu);
                }
                count++;

            }
        }
        return count;
    }

    private void processForNewBundles(List<IInstallableUnit> newIUs, Set<String> curinboth) throws ProvisionException,
            URISyntaxException {
        for (IInstallableUnit curiu : getAllIUs().toSet()) {

            // we exclude feature groups here, so they can be in their
            // own list, for better focus
            // TODO: should we exclude "special" IUs from this version
            // test?
            if (!isCategory(curiu) && !isGroup(curiu)) {
                checkforNewInCurrent(curiu, newIUs, curinboth);

            }
        }
    }

    private void processForExtraReferences(List<IInstallableUnit> referenceOnly, Set<String> refinboth) throws ProvisionException,
            URISyntaxException {
        if (getAllReferenceIUs() != null) {
            for (IInstallableUnit refiu : getAllReferenceIUs().toSet()) {

                // we exclude feature groups here, so they can be in their
                // own list, for better focus
                // TODO: should we exclude "special" IUs from this version
                // test?
                if (!isCategory(refiu) && !isGroup(refiu)) {
                    // should not be any, going from major release to
                    // service release
                    checkforExtraReferences(refiu, referenceOnly, refinboth);

                }
            }
        }
    }

    private void checkforNewInCurrent(IInstallableUnit curiu, List<IInstallableUnit> newIUs, Set<String> curinboth)
            throws ProvisionException, URISyntaxException {
        String curiuID = curiu.getId();
        if (curiuID == null) {
            // TODO: throw exception here?
            System.out.println("iuID was unexpected null:" + curiu);
        } else {
            boolean foundMatch = false;
            if (getAllReferenceIUs() != null) {
                for (IInstallableUnit iu : getAllReferenceIUs().toSet()) {

                    if (curiuID.equals(iu.getId())) {
                        foundMatch = true;
                        break;
                    }
                }
            }
            if (foundMatch) {
                curinboth.add(curiu.getId());
            } else {
                newIUs.add(curiu);
            }
        }

    }

    private void checkforExtraReferences(IInstallableUnit refiu, List<IInstallableUnit> referenceOnly, Set<String> inboth)
            throws ProvisionException, URISyntaxException {
        String refiuID = refiu.getId();
        if (refiuID == null) {
            // TODO: throw exception here?
            System.out.println("iuID was unexpected null:" + refiu);
        } else {
            boolean foundMatch = false;
            for (IInstallableUnit iu : getAllIUs().toSet()) {

                if (refiuID.equals(iu.getId())) {
                    foundMatch = true;
                    break;
                }
            }
            if (foundMatch) {
                inboth.add(refiu.getId());
            } else {
                referenceOnly.add(refiu);
            }
        }

    }

    private void printLinesIUs(FileWriter out, List<IInstallableUnit> iuList) throws IOException {
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

    private void printTableIUs(FileWriter out, List<IInstallableUnit> iuListCur, IQueryResult<IInstallableUnit> iuListRefs)
            throws IOException {

        out.write("<p>Count: " + iuListCur.size() + EOL);
        if (!iuListCur.isEmpty()) {
            Collections.sort(iuListCur);
            printStartTable(out, "");
            printRowln(out, "<th>" + "IU id" + "</th><th>" + "Reference (old) version" + "</th><th>" + "Current (new) version"
                    + "</th>");
            for (IInstallableUnit curInstallableUnit : iuListCur) {
                IInstallableUnit refIInstallableUnit = getIUNamed(iuListRefs, curInstallableUnit.getId());
                printLineRowItem(out, curInstallableUnit, refIInstallableUnit);
            }
            printEndTable(out);
        }
    }

    private IInstallableUnit getIUNamed(IQueryResult<IInstallableUnit> iuListRefs, String id) {
        IInstallableUnit result = null;
        for (IInstallableUnit iInstallableUnit : iuListRefs) {
            if (iInstallableUnit.getId().equals(id)) {
                result = iInstallableUnit;
                break;
            }
        }
        return result;
    }

}
