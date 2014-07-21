/**
 * 
 */
package org.eclipse.simrel.tests.common;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.checker.ReportType;

/**
 * @author dhuebner
 *
 */
public class CheckReport {
	private ReportType type = ReportType.INFO;
	private long timeMs;
	private IInstallableUnit iu;
	private String checkerId;
	private String message;

	public CheckReport(Class<?> checkerId, IInstallableUnit iu) {
		super();
		this.iu = iu;
		this.checkerId = checkerId.getName();
	}

	public void setType(ReportType type) {
		this.type = type;
	}

	public ReportType getType() {
		return type;
	}

	public IInstallableUnit getIU() {
		return this.iu;
	}

	public String getCheckerId() {
		return this.checkerId;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public long getTimeMs() {
		return timeMs;
	}

	public void setTimeMs(long timeMs) {
		this.timeMs = timeMs;
	}
}
