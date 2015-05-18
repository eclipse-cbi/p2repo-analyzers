package org.eclipse.simrel.tests;

import static org.eclipse.simrel.tests.RepoTestsConfiguration.REFERENCE_REPO_PARAM;
import static org.eclipse.simrel.tests.RepoTestsConfiguration.REPORT_OUTPUT_DIR_PARAM;
import static org.eclipse.simrel.tests.RepoTestsConfiguration.REPORT_REPO_DIR_PARAM;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class RepoReportApplication implements IApplication {

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
        boolean testfailures = false;

        BuildRepoTests runAllReports = new BuildRepoTests(configurations);

        testfailures = runAllReports.execute();

        if (testfailures) {
            appresult = new Integer(-1);
            System.out.println("Report tests failed");
        } else {
            System.out.println("Reports completed as expected");
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

    public void stop() {
        // nothing special

    }

}
