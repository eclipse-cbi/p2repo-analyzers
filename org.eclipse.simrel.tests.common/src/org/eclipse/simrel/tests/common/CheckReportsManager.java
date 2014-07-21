/**
 * 
 */
package org.eclipse.simrel.tests.common;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

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
		getReports().forEach(report -> new ConsoleReporter().dumpReport(report));
	}

	@Override
	public void accept(CheckReport report) {
		queue.add(report);
	}

	class ConsoleReporter implements ICheckReporter {

		@Override
		public void dumpReport(CheckReport report) {
			String string;
			if (report != null) {
				String time = new SimpleDateFormat("hh:mm:ss-SSS").format(new Date(report.getTimeMs()));
				string = time + " -> " + report.getCheckerId() + " " + report.getIU().getId() + " " + report.getType()
						+ ":" + report.getMessage();
			} else {
				string = System.currentTimeMillis() + " -> ERROR: Null report";
			}
			System.out.println(string);
		}
	}
}
