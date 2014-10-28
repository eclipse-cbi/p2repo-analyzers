/**
 * 
 */
package org.eclipse.simrel.tests.common.checker;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.utils.IUUtil;
import org.osgi.framework.Constants;

/**
 * @author dhuebner
 *
 */
public class BREEChecker implements IArtifactChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu,
			File child) {
		CheckReport report = new CheckReport(BREEChecker.class, iu);
		try {
			@SuppressWarnings("deprecation")
			String bree = IUUtil.getBundleManifestEntry(child, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			boolean needsBree = needsBree(child);
			if ((bree != null) && (bree.length() > 0)) {
				// has BREE, confirm is java file
				if (needsBree) {
					report.setType(ReportType.INFO);
					report.setCheckResult(bree);
				} else {
					report.setType(ReportType.BAD_GUY);
					report.setCheckResult("None Java with BREE: " + bree);
				}
			} else {
				// no BREE, confirm is non-java
				if (needsBree) {
					report.setType(ReportType.BAD_GUY);
					report.setCheckResult("Java without BREE");
				}
			}
		} catch (SecurityException e) {
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=378764
			report.setType(ReportType.NOT_IN_TRAIN);
			report.setCheckResult("Invalid jar: " + child.getName());
		}
		if (report.getCheckResult() != null) {
			report.setTimeMs(System.currentTimeMillis());
			consumer.accept(report);
		}
	}

	private boolean needsBree(File child) {
		return exportsPackages(child);
	}

	private boolean exportsPackages(File child) {
		String entry = IUUtil.getBundleManifestEntry(child, Constants.EXPORT_PACKAGE);
		if (entry != null && !entry.isEmpty()) {
			return true;
		}
		return false;
	}
}
