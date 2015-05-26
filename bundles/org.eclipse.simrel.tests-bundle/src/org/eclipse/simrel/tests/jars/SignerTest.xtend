/** 
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 */
package org.eclipse.simrel.tests.jars

import com.google.common.base.Strings
import java.io.File
import java.io.IOException
import java.util.Collection
import java.util.Properties
import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ForkJoinPool
import java.util.function.Consumer
import org.eclipse.simrel.tests.RepoTestsConfiguration
import org.eclipse.simrel.tests.common.ReportType
import org.eclipse.simrel.tests.utils.BundleJarUtils
import org.eclipse.simrel.tests.utils.CompositeFileFilter
import org.eclipse.simrel.tests.utils.JARFileNameFilter
import org.eclipse.simrel.tests.utils.PackGzFileNameFilter
import org.eclipse.simrel.tests.utils.PlainCheckReport
import org.eclipse.simrel.tests.utils.ReportWriter
import org.eclipse.simrel.tests.utils.VerifyStep

class SignerTest extends TestJars {
	static final String UNSIGNED_FILENAME = "unsigned8.txt"
	static final String SIGNED_FILENAME = "verified8.txt"
	static final String KNOWN_UNSIGNED = "knownunsigned8.txt"

	new(RepoTestsConfiguration configurations) {
		super(configurations)
	}

	/**
	 * @return <code>true</code> if errors were found 
	 */
	def boolean verifySignatures() throws IOException {
		val checkReports = new CopyOnWriteArraySet<PlainCheckReport>()
		if (!VerifyStep.canVerify()) {
			System.err.println("jarsigner is not available. Can not check.")
			return true
		} else {
			checkJars(new File(featureDirectory), 'feature', checkReports)
			checkJars(new File(bundleDirectory), 'plugin', checkReports)
		}
		val containsErrors = checkReports.exists[type == ReportType.NOT_IN_TRAIN]
		printSummary(checkReports)
		return containsErrors
	}

	def checkJars(File dirToCheck, String iuType, CopyOnWriteArraySet<PlainCheckReport> reports) {
		val jars = dirToCheck.listFiles(CompositeFileFilter.create(new JARFileNameFilter, new PackGzFileNameFilter))
		val forkJoinPool = new ForkJoinPool(64);
		forkJoinPool.submit [
			jars.parallelStream.forEach(new SignerCheck(reports, iuType))
		].get()

	}

	def private void printSummary(Set<PlainCheckReport> reports) throws IOException {
		var ReportWriter info = createNewReportWriter(SIGNED_FILENAME)
		var ReportWriter warn = createNewReportWriter(KNOWN_UNSIGNED)
		var ReportWriter error = createNewReportWriter(UNSIGNED_FILENAME)
		try {
			val featuresCount = reports.filter[iuType.equals('feature')].size
			info.
				writeln(
			'''
					Jars checked: «reports.size». «featuresCount» features and «reports.size-featuresCount» plugins.
					Valid signatures: «reports.filter[type==ReportType.INFO].size».
					Explicitly excluded from signing: «reports.filter[type==ReportType.BAD_GUY].size». See «KNOWN_UNSIGNED» for more details.
					Invalid or missing signature: «reports.filter[type==ReportType.NOT_IN_TRAIN].size». See «UNSIGNED_FILENAME» for more details.
				''')

			val longestFileName = reports.sortBy[fileName.length].last.fileName.length
			for (report : reports.sortBy[fileName]) {
				val indent = Strings.repeat(" ", longestFileName - report.fileName.length)
				val trailing = Strings.repeat(' ', 10 - report.iuType.length)
				val line = ''' «report.fileName»«indent»	«report.iuType»«trailing»	«report.checkResult»'''
				switch (report.type) {
					case INFO: info.writeln(line)
					case NOT_IN_TRAIN: error.writeln(line)
					case BAD_GUY: warn.writeln(line)
				}
			}
		} finally {
			info.close()
			warn.close()
			error.close()
		}
	}

	def protected createNewReportWriter(String filename) {
		return new ReportWriter('''«getReportOutputDirectory()»/«filename»''')
	}

	final static class SignerCheck implements Consumer<File> {
		final Collection<PlainCheckReport> reports
		final String iuTypeName

		package new(Collection<PlainCheckReport> reports, String iuTypeName) {
			this.reports = reports
			this.iuTypeName = iuTypeName
		}

		override accept(File file) {
			val checkReport = new PlainCheckReport
			checkReport.fileName = file.name
			checkReport.iuType = iuTypeName

			var File fileToCheck = file
			if (fileToCheck.getName().endsWith(PackGzFileNameFilter.EXTENSION_PACEKD_JAR)) {
				try {
					fileToCheck = BundleJarUtils.unpack200gz(fileToCheck)
				} catch (IOException e) {
					checkReport.type = ReportType.
						NOT_IN_TRAIN
					checkReport.checkResult = '''Unable to unpack «file.getAbsolutePath()». Can not check signature. «e.getMessage()»'''
					return
				}
			}

			// signing disabled: jarprocessor.exclude.sign = true
			var Properties eclipseInf = BundleJarUtils.getEclipseInf(fileToCheck)
			if (Boolean.valueOf(eclipseInf.getProperty("jarprocessor.exclude.sign", "false"))) {
				// skip check
				checkReport.type = ReportType.BAD_GUY
				checkReport.checkResult = "Jar was excluded from signing using the eclipse.inf entry."
			} else {
				var errorOut = new StringBuilder()
				var warningOut = new StringBuilder()
				val verified = VerifyStep.verify(fileToCheck, errorOut, warningOut)
				if (!verified) {
					checkReport.type = ReportType.NOT_IN_TRAIN
					checkReport.checkResult = errorOut.toString
				} else {
					var message = "jar verified"
					if (warningOut.length() > 0) {
						checkReport.type = ReportType.INFO
						message = warningOut.toString
					}
					checkReport.checkResult = message
				}
			}
			reports.add(checkReport)
		}

	}
}
		