/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.common;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
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
	private String additionalData;
	private String iuVersion;
	private String artifactKey;

	public CheckReport(final Class<?> checkerId, final IInstallableUnit iu) {
		super();
		this.iu = iu;
		this.checkerId = checkerId.getName();
		if (iu != null)
			this.iuVersion = iu.getVersion().getOriginal();
		this.setTimeMs(System.currentTimeMillis());
	}

	public CheckReport(final Class<?> checkerId, final IInstallableUnit iu, final IArtifactKey artifactKey) {
		this(checkerId, iu);
		this.artifactKey = artifactKey.toExternalForm();
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(type).append(": ").append(getVersionedId());
		if (artifactKey != null) {
			builder.append(" (").append(artifactKey).append(")");
		}
		builder.append(" <- ").append(checkerId).append(": ").append(checkResult).append(' ').append(additionalData)
				.append(" " + timeMs);
		return builder.toString();
	}

	public String getVersionedId() {
		return iu.getId() + (iuVersion != null ? "_" + iuVersion : "");
	}

	/**
	 * @return the additionalData provided by a checker
	 */
	public String getAdditionalData() {
		return additionalData;
	}

	public void setAdditionalData(String additionalData) {
		this.additionalData = additionalData;
	}

	/**
	 * @return the iuVersion
	 */
	public String getIuVersion() {
		return iuVersion;
	}

	/**
	 * @return the artifactKey
	 */
	public String getArtifactKey() {
		return artifactKey;
	}
}
