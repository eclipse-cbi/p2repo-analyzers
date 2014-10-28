/**
 * 
 */
package org.eclipse.simrel.tests.common;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.eclipse.simrel.tests.common.checker.ReportType;

/**
 * @author dhuebner
 *
 */
public class CheckReportsManager implements Consumer<CheckReport> {

	private ConcurrentLinkedQueue<CheckReport> queue = new ConcurrentLinkedQueue<>();

	public int reported() {
		return queue.size();
	}

	public ConcurrentLinkedQueue<CheckReport> getReports() {
		return queue;
	}

	public void dumpReports() {
		getReports()
				.forEach(report -> new ConsoleReporter().dumpReport(report));
	}

	@Override
	public void accept(CheckReport report) {
		queue.add(report);
	}

	class ConsoleReporter implements ICheckReporter {

		private boolean dumpTime = false;

		@Override
		public void dumpReport(CheckReport report) {
			if (report == null) {
				System.out.println("ERROR: Null report");
			}

			if (report.getType() != ReportType.INFO) {
				String time = "";
				if (dumpTime) {
					time = new SimpleDateFormat("hh:mm:ss-SSS")
							.format(new Date(report.getTimeMs()));
				}
				String message = report.getType() + ": "
						+ report.getCheckResult() + " "
						+ report.getIU().getId() + "  <- " + time + " "
						+ report.getCheckerId();
				System.out.println(message);
			}
		}
	}
}
