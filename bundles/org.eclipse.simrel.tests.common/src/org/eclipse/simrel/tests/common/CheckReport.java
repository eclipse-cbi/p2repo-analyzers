/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @author dhuebner - Initial contribution and API
 */
public class CheckReport {
	private ReportType type = ReportType.INFO;
	private long timeMs;
	private IInstallableUnit iu;
	private String checkerId;
	private String checkResult;

	public CheckReport(final Class<?> checkerId, final IInstallableUnit iu) {
		super();
		this.iu = iu;
		this.checkerId = checkerId.getName();
		this.setTimeMs(System.currentTimeMillis());
	}

	public void setType(final ReportType type) {
		this.type = type;
	}

	public ReportType getType() {
		return this.type;
	}

	public IInstallableUnit getIU() {
		return this.iu;
	}

	public String getCheckerId() {
		return this.checkerId;
	}

	public void setCheckResult(final String result) {
		this.checkResult = result;
	}

	public String getCheckResult() {
		return this.checkResult;
	}

	public long getTimeMs() {
		return this.timeMs;
	}

	public void setTimeMs(final long timeMs) {
		this.timeMs = timeMs;
	}
}
