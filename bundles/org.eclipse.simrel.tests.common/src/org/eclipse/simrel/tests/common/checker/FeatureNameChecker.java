/**
 * 
 */
package org.eclipse.simrel.tests.common.checker;

import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.utils.IUUtil;
import org.eclipse.simrel.tests.repos.FeatureNameLengths;

/**
 * @author dhuebner
 *
 */
public class FeatureNameChecker implements IInstalationUnitChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu) {
		// simulate what directory name would be, when installed
		if (IUUtil.isFeature(iu)) {
			String featureName = iu.getId().substring(0, iu.getId().length() - ".feature.group".length());
			String line = featureName + "_" + iu.getVersion();
			CheckReport checkReport = new CheckReport(FeatureNameChecker.class, iu);
			checkReport.setCheckResult(String.valueOf(line.length()));
			if (line.length() > FeatureNameLengths.MAX_CRITERIA) {
				checkReport.setCheckResult(line);
				checkReport.setType(ReportType.BAD_GUY);
			}
			checkReport.setTimeMs(System.currentTimeMillis());
			consumer.accept(checkReport);
		}
	}

}
