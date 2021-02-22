package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.IUIdComparator;

public class IUVersionCheckToReference extends TestRepo {

    public IUVersionCheckToReference(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    public boolean checkIUVersionsToReference() throws IOException, ProvisionException, URISyntaxException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> referenceOnly = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> newIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> decreasingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> matchingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsMajor = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsMinor = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsService = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsQualifierOnly = new ArrayList<IInstallableUnit>();
        Set<String> refinboth = new TreeSet<String>();
        Set<String> curinboth = new TreeSet<String>();

        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "versionChecks.html");
            outfileWriter = new FileWriter(outfile);
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

    private void processForDifferences(Set<String> curinboth, IQueryResult<IInstallableUnit> allIUs,
            IQueryResult<IInstallableUnit> allReferenceIUs, List<IInstallableUnit> decreasingVersions,
            List<IInstallableUnit> matchingVersions, List<IInstallableUnit> increaseVersionsMajor,
            List<IInstallableUnit> increaseVersionsMinor, List<IInstallableUnit> increaseVersionsService,
            List<IInstallableUnit> increaseVersionsQualifierOnly) {

        Set allCurrent = allIUs.toUnmodifiableSet();
        Set allRef = allReferenceIUs.toUnmodifiableSet();

        for (Iterator iterator = curinboth.iterator(); iterator.hasNext();) {
            String iuname = (String) iterator.next();
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

    private IInstallableUnit getIU(String iuname, Set allIUs) {
        IInstallableUnit result = null;
        for (Iterator iterator = allIUs.iterator(); iterator.hasNext();) {
            IInstallableUnit iu = (IInstallableUnit) iterator.next();
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
            for (Iterator iterator = refs.iterator(); iterator.hasNext();) {
                IInstallableUnit iiu = (IInstallableUnit) iterator.next();
                if (iiu.getId() == null || iiu.getId().isEmpty()) {
                    throw new RuntimeException("IIU had no (or empty) ID string: " + iiu);
                }
                count++;

            }
        }
        return count;
    }

    private void dump(List<IInstallableUnit> refinboth, String filename) throws IOException {
        Collections.sort(refinboth);
        FileWriter outfile = null;
        try {
            File out = new File(getReportOutputDirectory(), filename);
            outfile = new FileWriter(out);
            for (IInstallableUnit iInstallableUnit : refinboth) {
                outfile.write(iInstallableUnit.toString() + EOL);
            }
        } finally {
            if (outfile != null) {
                outfile.close();
            }
        }

    }

    private void dump(Set<String> inboth, String filename) throws IOException {
        FileWriter outfile = null;
        try {
            File out = new File(getReportOutputDirectory(), filename);
            outfile = new FileWriter(out);
            for (String iInstallableUnit : inboth) {
                outfile.write(iInstallableUnit.toString() + EOL);
            }
        } finally {
            if (outfile != null) {
                outfile.close();
            }
        }

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

    private void processForExtraReferences(List<IInstallableUnit> referenceOnly, Set refinboth) throws ProvisionException,
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
        if (iuListCur.size() > 0) {
            Collections.sort(iuListCur);
            printStartTable(out, "");
            printRowln(out, "<th>" + "IU id" + "</th><th>" + "Reference (old) version" + "</th><th>" + "Current (new) version"
                    + "</th>");
            for (Iterator iterator = iuListCur.iterator(); iterator.hasNext();) {
                IInstallableUnit curInstallableUnit = (IInstallableUnit) iterator.next();
                IInstallableUnit refIInstallableUnit = getIUNamed(iuListRefs, curInstallableUnit.getId());
                printLineRowItem(out, curInstallableUnit, refIInstallableUnit);
            }
            printEndTable(out);
        }
    }

    // private void printTableIUs(FileWriter out, Set<String> iuIds,
    // IQueryResult<IInstallableUnit> iuList,
    // IQueryResult<IInstallableUnit> iuListRefs) throws IOException {
    // // Comparator<? super IInstallableUnit> comparatorBundleName = new
    // // IUNameAndIdComparator();
    // // Comparator<? super IInstallableUnit> comparatorBundleName = new
    // // IUIdComparator();
    // List<String> iuIdList = new ArrayList<String>(iuIds);
    // Collections.sort(iuIdList);
    // out.write("<p>Count: " + iuIdList.size() + EOL);
    // printStartTable(out, "");
    // printRowln(out, "<th>" + "IU id" + "</th><th>" +
    // "Reference (old) version" + "</th><th>" + "Current (new) version"
    // + "</th>");
    //
    // for (String iuId : iuIdList) {
    // IInstallableUnit iu = getIUNamed(iuList, iuId);
    // IInstallableUnit iuRef = getIUNamed(iuListRefs, iuId);
    // printLineRowItem(out, iu, iuRef);
    // }
    // printEndTable(out);
    // }

    private IInstallableUnit getIUNamed(IQueryResult<IInstallableUnit> iuListRefs, String id) {
        IInstallableUnit result = null;
        for (Iterator iterator = iuListRefs.iterator(); iterator.hasNext();) {
            IInstallableUnit iInstallableUnit = (IInstallableUnit) iterator.next();
            if (iInstallableUnit.getId().equals(id)) {
                result = iInstallableUnit;
                break;
            }
        }
        return result;
    }

    public boolean checkIUVersionsToReferenceForFeatures() throws IOException, ProvisionException, URISyntaxException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> inreferenceOnly = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> newIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> decreasingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> matchingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsMajor = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsMinor = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsService = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> increaseVersionsQualifierOnly = new ArrayList<IInstallableUnit>();
        Set<String> refinboth = new TreeSet<String>();
        Set<String> curinboth = new TreeSet<String>();

        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "versionChecksFeatures.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());

            System.out.println("all feature groups IUs");
            System.out.println("Number in reference from raw count:" + rawcount(getAllReferenceGroupIUs()));
            if (getAllReferenceGroupIUs() != null) {
                System.out.println("Number in reference from set: " + getAllReferenceGroupIUs().toSet().size());
            }
            System.out.println("Number in current from raw count:" + rawcount(getAllGroupIUs()));
            System.out.println("Number in current from set: " + getAllGroupIUs().toSet().size());

            processForExtraReferencesFeatures(inreferenceOnly, refinboth);

            System.out.println("Number in reference missing from current: " + inreferenceOnly.size());
            System.out.println("Number in common in reference and current: " + refinboth.size());
            dump(inreferenceOnly, "inRefererenceOnly.txt");
            dump(refinboth, "inBothRefFeatures.txt");

            processForNewFeatures(newIUs, curinboth);

            System.out.println("Number in current missing from reference: " + newIUs.size());
            System.out.println("Number in common in reference and current: " + curinboth.size());

            dump(newIUs, "newInCurrent.txt");
            dump(curinboth, "inBothCurrentFeatures.txt");

            outfileWriter.write("<h1>feature.group IU changes</h1>" + EOL);
            outfileWriter.write("<p>Current Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            if (!getRepoURLForReference().isEmpty()) {
                outfileWriter.write("<p>Repository for reference ('repoURLForReference'): " + getRepoURLForReference() + "</p>"
                        + EOL);
            }
            outfileWriter.write("<h2>IUs in reference repo, but not current repo</h2>" + EOL);
            printLinesIUs(outfileWriter, inreferenceOnly);
            outfileWriter.write("<h2>IUs in current repo, but not reference repo</h2>" + EOL);
            printLinesIUs(outfileWriter, newIUs);

            processForDifferences(curinboth, getAllGroupIUs(), getAllReferenceGroupIUs(), decreasingVersions, matchingVersions,
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

            // printTableIUs(outfileWriter, curinboth, getAllGroupIUs(),
            // getAllReferenceGroupIUs());
            return true;
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

    private void processForExtraReferencesFeatures(List<IInstallableUnit> referenceOnly, Set<String> refinboth)
            throws ProvisionException, URISyntaxException {
        if (getAllReferenceIUs() != null) {
            for (IInstallableUnit refiu : getAllReferenceIUs().toSet()) {
                if (isGroup(refiu)) {
                    // should not be any, going from major release to
                    // service release
                    checkforExtraReferences(refiu, referenceOnly, refinboth);
                }
            }
        }
    }

    private void processForNewFeatures(List<IInstallableUnit> newIUs, Set<String> curinboth) throws ProvisionException,
            URISyntaxException {
        for (IInstallableUnit curiu : getAllGroupIUs().toSet()) {

            if (isGroup(curiu)) {
                checkforNewInCurrent(curiu, newIUs, curinboth);
            } else {
                throw new RuntimeException("Feature group (from get groupIUs) does not end with feature.group: " + curiu);
            }
        }
    }

}
