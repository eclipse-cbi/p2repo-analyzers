package org.eclipse.simrel.tests;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class RepoReportApplication implements IApplication {

    public Object start(IApplicationContext context) throws Exception {
        Object appresult = IApplication.EXIT_OK;
        // TODO: eventually may want test failures to be an array, or map, so
        // we'd know exactly what failed.
        // but for now, all "tests" return "not failed"
        boolean testfailures = false;
        BuildRepoTests runAllReports = new BuildRepoTests();

        // runAllReports.setMainOutputDirectory("/home/davidw/temp");
        // runAllReports.copyTemplateForIndexFile("/indexPending.html");

        // runAllReports.setDirectoryToCheck("/home/www/html/downloads/releases/juno/201202030900");
        // runAllReports.setDirectoryToCheckForReference("/home/www/html/downloads/releases/juno/201112160900");
        // runAllReports.setDirectoryToCheck("/home/davidw/temp/gtest");
        // runAllReports.setDirectoryToCheckForReference("/home/davidw/temp/gtest");

        // runAllReports.setDirectoryToCheck("/home/files/buildzips/orbit/S20120123151124/repository");
        // runAllReports.setDirectoryToCheckForReference("/home/files/buildzips/orbit/R20110523182458/repository");

        testfailures = runAllReports.execute();

        // runAllReports.copyTemplateForIndexFile("/indexmainpresign.html");

        if (testfailures) {
            appresult = new Integer(-1);
            System.out.println("Report tests failed");
        } else {
            System.out.println("Reports completed as expected");
        }

        return appresult;
    }

    public void stop() {
        // nothing special

    }

}
