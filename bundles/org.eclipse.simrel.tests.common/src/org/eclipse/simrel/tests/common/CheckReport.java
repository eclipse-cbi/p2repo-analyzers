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
	private String checkResult;

	public CheckReport(Class<?> checkerId, IInstallableUnit iu) {
		super();
		this.iu = iu;
		this.checkerId = checkerId.getName();
		this.setTimeMs(System.currentTimeMillis());
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

	public void setCheckResult(String result) {
		this.checkResult = result;
	}

	public String getCheckResult() {
		return checkResult;
	}

	public long getTimeMs() {
		return timeMs;
	}

	public void setTimeMs(long timeMs) {
		this.timeMs = timeMs;
	}
}
