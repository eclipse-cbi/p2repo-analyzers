/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.reporter;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.ReportType;

/**
 * @author dhuebner - Initial contribution and API
 */
public class CheckReportsManager implements Consumer<CheckReport> {

	private ConcurrentLinkedQueue<CheckReport> queue = new ConcurrentLinkedQueue<>();

	public int reported() {
		return this.queue.size();
	}

	public ConcurrentLinkedQueue<CheckReport> getReports() {
		return this.queue;
	}

	public void dumpReports() {
		List<CheckReport> sorted = new ArrayList<CheckReport>(getReports());
		Collections.sort(sorted, (CheckReport r1, CheckReport r2) -> {
			return r2.getType().compareTo(r1.getType());
		} );
		sorted.forEach(report -> new ConsoleReporter().dumpReport(report));
	}

	@Override
	public void accept(final CheckReport report) {
		this.queue.add(report);
	}

	class ConsoleReporter implements ICheckReporter {

		private boolean dumpTime = false;

		@Override
		public void dumpReport(final CheckReport report) {
			if (report == null) {
				System.out.println("ERROR: Null report");
			}

			if (report.getType() != ReportType.INFO) {
				String time = "";
				if (this.dumpTime) {
					time = new SimpleDateFormat("hh:mm:ss-SSS").format(new Date(report.getTimeMs()));
				}
				String message = report.getType() + ": " + report.getCheckResult() + " " + report.getIU().getId()
						+ "  <- " + time + " " + report.getCheckerId();
				System.out.println(message);
			}
		}
	}
}
