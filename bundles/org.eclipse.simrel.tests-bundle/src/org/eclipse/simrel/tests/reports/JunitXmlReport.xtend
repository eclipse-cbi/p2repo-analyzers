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
import static com.google.common.xml.XmlEscapers.*

/** 
 * @author dhuebner - Initial contribution and API
 */
class JunitXmlReport implements ICheckReporter {
	
	override void createReport(CheckReportsManager manager, IP2RepositoryAnalyserConfiguration configs) {
		val dataDir = new File(configs.reportOutputDir, "data");
		dataDir.mkdirs
		val writer = new PrintWriter('''«dataDir»/junit-report.xml''')
		val groupedByCheck = manager.reports.
			groupBy[checkerId]
		val xmlContent = '''
			<?xml version="1.0" encoding="UTF-8"?>
			<testrun name="Simrel report" project="org.eclipse.simrel.tests-bundle" tests="«manager.reports.size»" started="«manager.reports.size»" failures="0" errors="0" ignored="0">
				«FOR check : groupedByCheck.keySet»
					<testsuite name="«check.split('\\.').last»" time="0.001">
						«val checkedIUsById = groupedByCheck.get(check).groupBy[IU]»
						«FOR iu : checkedIUsById.keySet»
							<testcase name="check_«iu.id»" classname="«check»" time="0.0">
							««« Iterate over all reports for current IU »»
							«FOR report: checkedIUsById.get(iu)»
								«IF report.checkResult!=null»
								<«report.type.asTag»>
									«xmlAttributeEscaper.escape(report.checkResult)»«IF report.additionalData!=null» - «xmlAttributeEscaper.escape(report.additionalData)»«ENDIF»
								</«report.type.asTag»>
								«ENDIF»
							«ENDFOR»
							</testcase>
						«ENDFOR»
					</testsuite>
				«ENDFOR»
			</testrun>
		'''
		writer.append(xmlContent);
		writer.close
	}

	def asTag(ReportType type) {
		if (type == ReportType.NOT_IN_TRAIN) {
			return "failure"
		} else {
			return "system-out"
		}
	}

}
