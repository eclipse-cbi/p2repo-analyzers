package org.eclipse.simrel.tests.reports

import java.io.PrintWriter
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager
import org.eclipse.simrel.tests.common.reporter.ICheckReporter
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration

/** 
 * @author dhuebner - Initial contribution and API
 */
class OverviewReport implements ICheckReporter {

	override void createReport(CheckReportsManager manager, IP2RepositoryAnalyserConfiguration configs) {
		val writer = new PrintWriter(configs.dataOutputDir + '/overview.csv')
		writer.println('''IU id;ReportType;Checker Class;Checker output;Report creation Time''')
		manager.reports.sortBy[type].sortBy[checkerId].forEach [
			writer.println(
		'''«IU.id»;«type»;«checkerId»;«checkResult»;«timeMs»''')
		]
		writer.close
	}
}