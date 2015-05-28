/** 
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.simrel.tests.reports

import java.io.PrintWriter
import org.eclipse.simrel.tests.common.CheckReport
import org.eclipse.simrel.tests.common.ReportType
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager
import org.eclipse.simrel.tests.common.reporter.ICheckReporter
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration

/** 
 * @author dhuebner - Initial contribution and API
 */
class HtmlReport implements ICheckReporter {
	val cssFileName = "html-report.css"
	val jsFileName = "html-report.js"

	override void createReport(CheckReportsManager manager, IP2RepositoryAnalyserConfiguration configs) {
		val writer = new PrintWriter('''«configs.reportOutputDir»/errors-and-moderate_warnings.html''')
		val allreports = manager.reports
		val xmlContent = '''
			<html>
			<head>
			<link rel="stylesheet" href="./data/«cssFileName»"/>
			<script src="http://code.jquery.com/jquery-1.11.3.min.js"></script>
			<script src="./data/«jsFileName»"></script>
			</head>
			<body>
				«htmlTable(ReportType.NOT_IN_TRAIN,allreports)»
				<br>
				«htmlTable( ReportType.BAD_GUY,allreports)»
			</body>
			</html>
		'''
		writer.append(xmlContent).close

		val warnWriter = new PrintWriter('''«configs.reportOutputDir»/warnings.html''')
		val warnContent = '''
			<html>
			<head>
			<link rel="stylesheet" href="./data/«cssFileName»"/>
			<script src="http://code.jquery.com/jquery-1.11.3.min.js"></script>
			<script src="./data/«jsFileName»"></script>
			</head>
			<body>
				«htmlTable(ReportType.WARNING,allreports)»
			</body>
			</html>
		'''
		warnWriter.append(warnContent).close

		// other
		addCssFile(configs)
		addJsFile(configs)
	}

	def htmlTable(ReportType reportType, Iterable<CheckReport> allreports) {
		val reports = allreports.filter[type == reportType]
		val groupbyIU = reports.groupBy[IU]
		val checkerIds = allreports.map[checkerId].toSet.sort
		val html = '''
			<h3 class="«reportType.asCssClass»">Installation units with «reportType.asHeaderTitle»s («reports.size»)</h3>
			
			<table id="table_«reportType.asCssClass»">
				<thead>
					<tr>
						<td>Id</td>
						<td>Version</td>
						«FOR checker : checkerIds»
							<td title="«checker»">
							«checker.abbreviation»&nbsp;
							<input type="checkbox" name="checker" class="«reportType.asCssClass»_toggler" checker="«checker.abbreviation»" checked="true">
							</td>	
						«ENDFOR»
					</tr>
				</thead>
				<tbody>
				«FOR iu : groupbyIU.keySet.sortBy[id]»
					«val iuReports = allreports.filter[IU==iu]»
					<tr class="«iu.id»_«iu.version.original»">
						<td>«iu.id»</td>
						<td>«iu.version.original»</td>
						«FOR checker:checkerIds»
							«val report = iuReports.filter[checkerId==checker].head»
							<td title="«report.asDescription»" class="«report.asCssClass»" data-result="«reportType.asCssClass»_«report.asCssClass»_«checker.abbreviation»" data-checker="«checker.abbreviation»">«report.asStatus»</td>	
						«ENDFOR»
					</tr>
				«ENDFOR»
				</tbody>
			</table>
		'''
		return html
	}

	def addJsFile(IP2RepositoryAnalyserConfiguration configs) {
		val writer = new PrintWriter('''«configs.dataOutputDir»/«jsFileName»''')
		val types = ReportType.values
		for (type : types) {
			writer.append(
		'''
				$(document).ready(function() {
					$(".«type.asCssClass»_toggler").click(function(e) {
						var targets = $('*[data-result="«type.asCssClass»_«type.asCssClass»_' + $(this).attr('checker')+'"]');
						var state = $(this).attr('checked')
						targets.each(function() {
							var tr = $(this).parent();
							tr.toggle(state)
						});
					});
				});
			''')
		}
		writer.close
	}

	def addCssFile(IP2RepositoryAnalyserConfiguration configs) {
		val writer = new PrintWriter('''«configs.dataOutputDir»/«cssFileName»''')
		writer.append('''
			table {
				min-width: 79%;
			}
			thead {
			    padding: 2px;
			    background-color: #E8E8E8;
			}
			td {
			    padding: 2px;
			}
		''')
		for (type : ReportType.values) {
			writer.append(
			'''
				.«type.asCssClass» {
					text-align: center;
					background-color: #«type.asBgColor»;
				}
			''')
		}
		writer.close

	}

	def asBgColor(ReportType type) {
		switch (type) {
			case NOT_IN_TRAIN: {
				'FFCCCC'
			}
			case BAD_GUY: {
				'FFCC66'
			}
			case WARNING: {
				'FFFFCC'
			}
			case INFO: {
				'CCFFCC'
			}
		}
	}

	def asCssClass(CheckReport report) {
		if (report == null)
			return 'skipped_check'
		asCssClass(report.type)
	}

	def asCssClass(ReportType type) {
		switch (type) {
			case NOT_IN_TRAIN: {
				'error_result'
			}
			case BAD_GUY: {
				'moderate_warning_result'
			}
			case WARNING: {
				'warning_result'
			}
			case INFO: {
				'info_result'
			}
		}
	}

	def asHeaderTitle(ReportType type) {
		switch (type) {
			case NOT_IN_TRAIN: {
				'Error'
			}
			case BAD_GUY: {
				'Moderate Warning'
			}
			case WARNING: {
				'Warning'
			}
			case INFO: {
				'Info'
			}
		}
	}

	def abbreviation(String string) {
		var simpleName = string
		val dotIndex = string.lastIndexOf('.')
		if (dotIndex >= 0 && dotIndex < string.length) {
			simpleName = string.substring(dotIndex + 1)
		}
		return simpleName.replaceAll("([A-Z]+)((?![A-Z])\\w)+", "$1")
	}

	def asDescription(CheckReport report) {
		if (report == null) {
			return 'any reports'
		} else {
			val result = if(report.checkResult.nullOrEmpty) 'passed' else report.checkResult
			return '''«result»«if(!report.additionalData.nullOrEmpty)' - '+report.additionalData»'''
		}
	}

	def asStatus(CheckReport report) {
		if (report == null) {
			return '&nbsp;'
		}
		switch (report.type) {
			case NOT_IN_TRAIN: {
				'--'
			}
			case BAD_GUY: {
				'-'
			}
			case WARNING: {
				'+'
			}
			case INFO: {
				'++'
			}
		}
	}

}
