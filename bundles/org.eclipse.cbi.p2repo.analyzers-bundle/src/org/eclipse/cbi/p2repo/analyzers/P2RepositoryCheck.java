/**
 * 
 */
package org.eclipse.cbi.p2repo.analyzers;

import java.util.List;

import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryAnalyser;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;
import org.eclipse.cbi.p2repo.analyzers.common.checker.CheckerRegistry;
import org.eclipse.cbi.p2repo.analyzers.common.reporter.CheckReportsManager;
import org.eclipse.cbi.p2repo.analyzers.common.reporter.ICheckReporter;
import org.eclipse.cbi.p2repo.analyzers.common.utils.IUUtil;
import org.eclipse.cbi.p2repo.analyzers.reports.HtmlReport;
import org.eclipse.cbi.p2repo.analyzers.reports.JunitXmlReport;
import org.eclipse.cbi.p2repo.analyzers.reports.OverviewReport;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;

import com.google.common.base.Stopwatch;

/**
 * @author dhuebner
 *
 */
public class P2RepositoryCheck {
    private List<ICheckReporter> reporter = List.of(new JunitXmlReport(), new OverviewReport(), new HtmlReport());

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
