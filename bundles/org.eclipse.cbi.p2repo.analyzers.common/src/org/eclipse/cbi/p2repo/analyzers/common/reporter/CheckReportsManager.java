/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.common.reporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;

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
		});
		sorted.forEach(report -> new ConsoleReporter().dumpReport(report));
	}

	@Override
	public void accept(final CheckReport report) {
		this.queue.add(report);
	}

	public Stream<CheckReport> reportsByCheckerId(final String checkerId) {
		Stream<CheckReport> stream = getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(checkerId));
		return featureReports;
	}

	class ConsoleReporter implements ICheckReportDumper {

		@Override
		public void dumpReport(final CheckReport report) {
			if (report == null) {
				System.out.println("ERROR: Null report");

			} else if (report.getType() != ReportType.INFO) {
				System.out.println(report.toString());
			}
		}
	}
}
