/** 
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.simrel.tests.reports

import java.io.PrintWriter
import org.eclipse.simrel.tests.common.ReportType
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager
import org.eclipse.simrel.tests.common.reporter.ICheckReporter
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration
import java.io.File

/** 
 * @author dhuebner - Initial contribution and API
 */
class JunitXmlReport implements ICheckReporter {

	override void createReport(CheckReportsManager manager, IP2RepositoryAnalyserConfiguration configs) {
		val dataDir = new File(configs.reportOutputDir, "data");
		dataDir.mkdirs
		val writer = new PrintWriter('''«dataDir»/junit-report.xml''')
		val groupedByIU = manager.reports.
			groupBy[IU]
		val xmlContent = '''
			<?xml version="1.0" encoding="UTF-8"?>
			<testrun name="Simrel report" project="org.eclipse.simrel.tests-bundle" tests="«manager.reports.size»" started="«manager.reports.size»" failures="0" errors="0" ignored="0">
				«FOR iu : groupedByIU.keySet»
					<testsuite name="«iu.id»" time="0.001">
						«val groupByChecker = groupedByIU.get(iu).groupBy[checkerId]»
						«FOR checker : groupByChecker.keySet»
							<testcase name="check«checker.split('\\.').last»" classname="«checker»" time="0.0">
							«IF groupByChecker.get(checker).filter[type==ReportType.NOT_IN_TRAIN].size > 0»
								<failure>
								«FOR error: groupByChecker.get(checker).filter[type==ReportType.NOT_IN_TRAIN]»
									«error.checkResult» reported by: «error.checkerId»
								«ENDFOR»
								</failure>
							«ENDIF»
							</testcase>
						«ENDFOR»
					</testsuite>
				«ENDFOR»
			</testrun>
		'''
		writer.append(xmlContent);
		writer.close
	}
}
