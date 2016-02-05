/**
 * 
 */
package org.eclipse.simrel.tests;

import java.util.List;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.simrel.tests.common.P2RepositoryAnalyser;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.checker.CheckerRegistry;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.eclipse.simrel.tests.common.reporter.ICheckReporter;
import org.eclipse.simrel.tests.common.utils.IUUtil;
import org.eclipse.simrel.tests.reports.HtmlReport;
import org.eclipse.simrel.tests.reports.JunitXmlReport;
import org.eclipse.simrel.tests.reports.OverviewReport;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * @author dhuebner
 *
 */
public class P2RepositoryCheck {
    private List<ICheckReporter> reporter = Lists.newArrayList(new JunitXmlReport(), new OverviewReport(), new HtmlReport());

    public boolean runChecks(RepoTestsConfiguration configuration) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            P2RepositoryDescription p2Repo = IUUtil.createRepositoryDescription(configuration.getReportRepoURI());
            System.out.println("create repo descr: " + stopwatch);
            stopwatch.reset().start();

            CheckerRegistry registry = new CheckerRegistry();
            P2RepositoryAnalyser analyser = new P2RepositoryAnalyser();
            CheckReportsManager manager = new CheckReportsManager();
            analyser.analyse(p2Repo, registry, manager);
            System.out.println("run analyse: " + stopwatch);
            stopwatch.reset().start();
            for (ICheckReporter iCheckReporter : reporter) {
                iCheckReporter.createReport(manager, configuration);
            }
            System.out.println("dump reports: " + stopwatch);
            System.out.println("Reports output: " + configuration.getReportOutputDir());
            stopwatch.stop();
        } catch (ProvisionException | OperationCanceledException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}
