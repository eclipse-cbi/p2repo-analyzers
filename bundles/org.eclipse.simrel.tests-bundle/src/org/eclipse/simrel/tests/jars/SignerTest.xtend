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

import java.io.File
import java.io.IOException
import java.util.Collection
import java.util.Properties
import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.eclipse.simrel.tests.RepoTestsConfiguration
import org.eclipse.simrel.tests.common.ReportType
import org.eclipse.simrel.tests.utils.BundleJarUtils
import org.eclipse.simrel.tests.utils.CompositeFileFilter
import org.eclipse.simrel.tests.utils.JARFileNameFilter
import org.eclipse.simrel.tests.utils.PackGzFileNameFilter
import org.eclipse.simrel.tests.utils.PlainCheckReport
import org.eclipse.simrel.tests.utils.ReportWriter
import org.eclipse.simrel.tests.utils.VerifyStep
import com.google.common.base.Strings

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
		var int nFiles = jars.length
		// TODO: may have to tweak timePerFile, or the simple multiplication formula 
		// based on experience. But, primarily we just need a good safe "MAXIMUM" in 
		// case things go wrong. If thing go right, it will not blindly wait the MAXIMUM time.
		var int TIME_PER_FILE = 2
		var int TOTAL_WAIT_SECONDS = nFiles * TIME_PER_FILE
		var ExecutorService threadPool = Executors.newFixedThreadPool(64)
		for (File file : jars) {
			threadPool.execute(new SignerRunnable(reports, file, iuType))
		}
		threadPool.shutdown()
		try {
			// initial "wait until done" is a funtion of how many files 
			// there are to verify. We'll allow about 2 seconds per file, 
			// which for 1500 files is about 50 minutes. That is a little less
			// than how long it would take if executing on one thread, so executing 
			// on a large number (e.g. 64) would be faster (if the hardward is up for 
			// it). 
			// TODO: consider a "submit" and then loop/wait checking for Futures.isDone.
			// it _might_ be a better idiom for this case? That is, allow more control? 
			// or "assessment" of what's taking a long time? But even then, would need some large, 
			// "we've waited long enough" time to be specified. 
			if (!threadPool.awaitTermination(TOTAL_WAIT_SECONDS, TimeUnit.SECONDS)) {
				threadPool.shutdownNow() // Cancel currently executing
				// tasks
				// Wait a while for tasks to respond to being cancelled
				// Here is reasonable to have "short time" to wait, since 
				// something is incomplete anyway, if get to here.
				if (!threadPool.awaitTermination(5, TimeUnit.MINUTES))
					System.err.println("ThreadPool did not terminate within time limits.")
			}

		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			threadPool.shutdownNow() // Preserve interrupt status
			Thread.currentThread().interrupt()
		}
	}

	def private void printSummary(Set<PlainCheckReport> reports) throws IOException {
		var ReportWriter info = createNewReportWriter(SIGNED_FILENAME)
		var ReportWriter warn = createNewReportWriter(KNOWN_UNSIGNED)
		var ReportWriter error = createNewReportWriter(UNSIGNED_FILENAME)
		try {
			val featuresCount = reports.filter[iuType.equals('feature')].size
			info.writeln(
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

	final static class SignerRunnable implements Runnable {
		final Collection<PlainCheckReport> reports
		final File file
		final String iuTypeName

		package new(Collection<PlainCheckReport> reports, File file, String iuTypeName) {
			this.reports = reports
			this.file = file
			this.iuTypeName = iuTypeName
		}

		override void run() {
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
		