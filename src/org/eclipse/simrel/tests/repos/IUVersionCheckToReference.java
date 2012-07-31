package org.eclipse.simrel.tests.repos;

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
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.simrel.tests.utils.IUIdComparator;

public class IUVersionCheckToReference extends TestRepo {

    public boolean checkIUVersionsToReference() throws IOException, ProvisionException, URISyntaxException {
        FileWriter outfileWriter = null;
        File outfile = null;
        List<IInstallableUnit> referenceOnly = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> decreasingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> newIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> matchingVersions = new ArrayList<IInstallableUnit>();
        Set<String> refinboth = new TreeSet<String>();
        Set<String> curinboth = new TreeSet<String>();

        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "versionChecks.html");
            outfileWriter = new FileWriter(outfile);
            System.out.println("output: " + outfile.getAbsolutePath());

            System.out.println("all IUs");
            System.out.println("Number in reference from raw count:" + rawcount(getAllReferenceIUs()));
            if (getAllReferenceIUs() != null) {
                System.out.println("Number in reference from set:" + getAllReferenceIUs().toSet().size());
            }
            System.out.println("Number in current from raw count:" + rawcount(getAllIUs()));
            System.out.println("Number in current from set: " + getAllIUs().toSet().size());

            processForExtraReferences(referenceOnly, refinboth);

            System.out.println("Number in reference missing from current: " + referenceOnly.size());
            System.out.println("Number in common in reference and current: " + refinboth.size());
            dump(refinboth, "inBothRef.txt");

            processForNewBundles(newIUs, curinboth);

            System.out.println("Number in current missing from reference: " + newIUs.size());
            System.out.println("Number in common in reference and current: " + curinboth.size());
            dump(curinboth, "inBothCurrent.txt");

            outfileWriter.write("<h1>IU versions chagned</h1>" + EOL);
            outfileWriter.write("<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            if (getRepoURLForReference().length() > 0) {
                outfileWriter.write("<p>Repository for reference ('repoURLForReference'): " + getRepoURLForReference() + "</p>"
                        + EOL);
            }

            outfileWriter.write("<h2>Major: IUs in reference, but not in current repo</h2>" + EOL);
            printLinesIUs(outfileWriter, referenceOnly);
            // outfileWriter.write("<h2>Probably correct bundle name</h2>" +
            // EOL);
            // printLinesBundleName(outfileWriter, probablyCorrectBundleName);

            // if (incorrectBundleName.size() > 0) {
            // fail("Errors in naming or localization. For list, see " +
            // outfile.getName());
            // }

            // return incorrectBundleName.size() > 0;
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

    private int rawcount(IQueryResult<IInstallableUnit> refs) {
        int count = 0;
        if (refs != null) {
            for (Iterator iterator = refs.iterator(); iterator.hasNext();) {
                IInstallableUnit iiu = (IInstallableUnit) iterator.next();
                if (iiu.getId() == null || iiu.getId().length() == 0) {
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
    private void printLinesCommonIUs(FileWriter out, Set<String> iuIds, IQueryResult<IInstallableUnit> iuList, IQueryResult<IInstallableUnit> iuListRefs) throws IOException {
        // Comparator<? super IInstallableUnit> comparatorBundleName = new
        // IUNameAndIdComparator();
        //Comparator<? super IInstallableUnit> comparatorBundleName = new IUIdComparator();
        List<String> iuIdList = new ArrayList<String>(iuIds);
        Collections.sort(iuIdList);
        out.write("<p>Count: " + iuIdList.size() + EOL);
        printStartTable(out, "");
        printRowln(out, "<th>" + "new less than reference" + "</th><th>" + "group IU id" +  "</th><th>" + "Reference (old) version" +  "</th><th>" + "Current (new) version" + "</th>");

        for (String iuId : iuIdList) {
            IInstallableUnit iu = getIUNamed(iuList, iuId);
            IInstallableUnit iuRef = getIUNamed(iuListRefs, iuId);
            printLineRowItem(out, iu, iuRef);
        }
        printEndTable(out);
    }
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
        List<IInstallableUnit> allreferenceIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> decreasingVersions = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> allCurrentIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> newIUs = new ArrayList<IInstallableUnit>();
        List<IInstallableUnit> matchingVersions = new ArrayList<IInstallableUnit>();
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
            if (getRepoURLForReference().length() > 0) {
                outfileWriter.write("<p>Repository for reference ('repoURLForReference'): " + getRepoURLForReference() + "</p>"
                        + EOL);
            }
            outfileWriter.write("<h2>group IUs in reference repo, but not current repo</h2>" + EOL);
            printLinesIUs(outfileWriter, inreferenceOnly);
            outfileWriter.write("<h2>group IUs in current repo, but not reference repo</h2>" + EOL);
            printLinesIUs(outfileWriter, newIUs);
            outfileWriter.write("<h2>group IUs in both current repo and reference repo</h2>" + EOL);
            
            printLinesCommonIUs(outfileWriter, curinboth, getAllGroupIUs(), getAllReferenceGroupIUs());
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
