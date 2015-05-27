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
import org.eclipse.simrel.tests.common.CheckReport

/** 
 * @author dhuebner - Initial contribution and API
 */
class HtmlReport implements ICheckReporter {

	override void createReport(CheckReportsManager manager, IP2RepositoryAnalyserConfiguration configs) {
		val writer = new PrintWriter('''«configs.reportOutputDir»/errors-and-warnings.html''')
		val allreports = manager.reports
		val xmlContent = '''
			<html>
			<body>
			«val errors = allreports.filter[type == ReportType.NOT_IN_TRAIN]»
			«IF !errors.empty»
				<b>Errors</b><br>
				«errors.htmlTable(allreports)»
			«ENDIF»
				<b>Warnings</b><br>
			«val warnings = allreports.filter[type == ReportType.BAD_GUY]»
			«warnings.htmlTable(allreports)»
			</body>
			</html>
		'''
		writer.append(xmlContent);
		writer.close
	}

	def htmlTable(Iterable<CheckReport> reports, Iterable<CheckReport> allreports) {
		val groupbyIU = reports.groupBy[IU]
		val checkerIds = allreports.map[checkerId].toSet.sort
		val html = '''
			<table>
				
				<tr>
					<td>IU Id</td>
					«FOR checker : checkerIds»
						<td>«checker.split('\\.').last»</td>	
					«ENDFOR»
				</tr>
			«FOR iu : groupbyIU.keySet.sortBy[id]»
				«val iuReports = allreports.filter[IU==iu]»
				<tr>
					<td>«iu.id»</td>
					«FOR checker:checkerIds»
						<td>«iuReports.filter[checkerId==checker].map[asStatus].join(',')»</td>	
					«ENDFOR»
				</tr>
			«ENDFOR»
			</table>
		'''
		return html
	}

	def asStatus(CheckReport report) {
		switch (report.type) {
			case NOT_IN_TRAIN: {
				'_'
			}
			case BAD_GUY: {
				'0'
			}
			case INFO: {
				'+'
			}
			default: {
				''
			}
		}
	}

}
