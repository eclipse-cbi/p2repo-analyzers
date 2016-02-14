package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.StringLengthComparator;

public class FeatureNameLengths extends TestRepo {

    public FeatureNameLengths(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private Map  distribution = null;
    public final static int  MAX_CRITERIA  = 100;
    private List longestNames = new ArrayList();

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    public static void main(String[] args) {
        FeatureNameLengths featureNameLengths = new FeatureNameLengths(RepoTestsConfiguration.createFromSystemProperties());
        featureNameLengths.setRepoURLToTest("/home/files/buildzips/junoRC3/wtp-repo");
        featureNameLengths.setMainOutputDirectory("/temp");
        try {
            featureNameLengths.testFeatureDirectoryLength();
        } catch (ProvisionException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void analyze(IQueryResult<IInstallableUnit> allIUs) {
        distribution = new HashMap<Integer, Integer>();
        for (IInstallableUnit iu : allIUs.toUnmodifiableSet()) {
            // simulate what directory name would be, when installed
            String featureName = iu.getId();
            String featureGroup = ".feature.group";
            int featureGroupLength = featureGroup.length();
            if (featureName.endsWith(featureGroup)) {
                featureName = featureName.substring(0, featureName.length() - featureGroupLength);
            }

            String line = featureName + "_" + iu.getVersion();
            tabulate(line.length());
            if (line.length() > MAX_CRITERIA) {
                longestNames.add(line);
            }
        }
    }

    private void printReport() throws IOException {
        String SPACER = "<br />=======================";
        FileWriter outfileWriter = null;
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        try {
            outfile = new File(testDirName, "featureDirectoryLengths.html");
            outfileWriter = new FileWriter(outfile);

            println(outfileWriter, "<p>Repository ('repoURLToTest'): " + getRepoURLToTest() + "</p>" + EOL);
            println(outfileWriter, "<br /><br />Distribution of Feature Directory Lengths:" + SPACER);

            Integer total = new Integer(0);
            Set keys = distribution.keySet();
            List<Integer> list = asSortedList(keys);
            for (Iterator iterator = list.iterator(); iterator.hasNext();) {
                Integer category = (Integer) iterator.next();
                Integer count = (Integer) distribution.get(category);
                println(outfileWriter, NBSP + category + NBSP + count);
                total = total + count;
            }
            println(outfileWriter, "=======================" + EOL);
            println(outfileWriter, NBSP + "Total features directory names: " + total + EOL);
            println(outfileWriter, "=======================" + EOL);
            if (longestNames.size() > 0) {

                println(outfileWriter, NBSP + "Features directory names with lengths above " + MAX_CRITERIA + EOL);
                Collections.sort(longestNames, new StringLengthComparator());
                for (int i = 0; i < longestNames.size(); i++) {
                    String line = (String) longestNames.get(i);
                    println(outfileWriter, line.length() + NBSP + line + EOL);
                }
            } else {
                println(outfileWriter, NBSP + " No feature directory names lengths were longer than the maxCriteria, "
                        + MAX_CRITERIA + EOL);
            }
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

    private void tabulate(int length) {
        Integer category = new Integer(length);
        Integer count = (Integer) distribution.get(category);
        if (count == null) {
            // our first occurance
            count = new Integer(1);
        } else {
            count = count + 1;
        }
        distribution.put(category, count);
    }

    public int testFeatureDirectoryLength() throws ProvisionException, URISyntaxException, IOException {
        IQueryResult<IInstallableUnit> allIUs = getAllGroupIUs();
        return checkFeatureDirLenths(allIUs);
    }

    private int checkFeatureDirLenths(IQueryResult<IInstallableUnit> allIUs) throws IOException {
        analyze(allIUs);
        printReport();
        return longestNames.size();
    }
}
