/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.checker.impl;

import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker;
import org.eclipse.simrel.tests.common.utils.IUUtil;

/**
 * @author dhuebner - Initial contribution and API
 */
public class FeatureNameChecker implements IInstalationUnitChecker {
	public static final int MAX_CRITERIA = 100;

	@Override
	public void check(final Consumer<? super CheckReport> consumer, final P2RepositoryDescription descr,
			final IInstallableUnit iu) {
		// simulate what directory name would be, when installed
		if (IUUtil.isFeature(iu)) {
			String featureName = iu.getId().substring(0, iu.getId().length() - ".feature.group".length());
			String line = featureName + "_" + iu.getVersion();
			CheckReport checkReport = new CheckReport(FeatureNameChecker.class, iu);
			checkReport.setCheckResult(String.valueOf(line.length()));
			if (line.length() > MAX_CRITERIA) {
				checkReport.setCheckResult(line);
				checkReport.setType(ReportType.BAD_GUY);
				checkReport.setAdditionalData("Feature name is to long (max=" + MAX_CRITERIA + ")");
			}
			checkReport.setTimeMs(System.currentTimeMillis());
			consumer.accept(checkReport);
		}
	}

}
