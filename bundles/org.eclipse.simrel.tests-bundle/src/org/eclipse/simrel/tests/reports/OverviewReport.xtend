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
		writer.println('''IU id;IU version;ReportType;Checker Class;Checker output;Checker additional data;Report creation Time''')
		manager.reports.sortBy[IU.id].sortWith[$1.type.ordinal.compareTo($0.type.ordinal)].forEach [
			writer.println(
		'''«IU.id»;«iuVersion»;«type»;«checkerId»;«checkResult»;«additionalData»;«timeMs»''')
		]
		writer.close
	}
}