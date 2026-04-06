package org.eclipse.cbi.p2repo.analyzers;

import static org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration.REFERENCE_REPO_PARAM;
import static org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration.REPORT_OUTPUT_DIR_PARAM;
import static org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration.REPORT_REPO_DIR_PARAM;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class RepoReportApplication implements IApplication {
    private final NumberFormat runtimeFormat = new DecimalFormat("#.000 s", new DecimalFormatSymbols(Locale.ENGLISH));

    @Override
    public Object start(IApplicationContext context) throws Exception {
        Object appresult = IApplication.EXIT_OK;
        RepoTestsConfiguration configurations = RepoTestsConfiguration.createFromSystemProperties();
        if (configurations.getReportRepoDir() == null) {
            dumpln(REPORT_REPO_DIR_PARAM + " - property is mandatory. Exit.");
            dumpHelpText();
            return appresult;
        }
        // TODO: eventually may want test failures to be an array, or map, so
        // we'd know exactly what failed.
        // but for now, all "tests" return "not failed"
        long start = System.currentTimeMillis();
        boolean testfailures = new BuildRepoTests(configurations).execute();
        String stopwatch = runtimeFormat.format((System.currentTimeMillis() - start) / 1000.0);
        if (testfailures) {
            appresult = Integer.valueOf(-1);
            System.out.println("Report tests failed. Took: " + stopwatch);
        } else {
            System.out.println("Reports completed as expected. Took: " + stopwatch);
        }

        return appresult;
    }

    private void dumpHelpText() {
        dumpln("Following properties are available:");
        dumpln("\t" + REPORT_REPO_DIR_PARAM + " - repository location to create reports.");
        dumpln("\t" + REPORT_OUTPUT_DIR_PARAM + " - report output location.");
        dumpln("\t" + REFERENCE_REPO_PARAM + " - reference repository location to compare with.");
    }

    private void dumpln(String string) {
        System.out.println(string);
    }

    @Override
    public void stop() {
        // nothing special

    }

}
