/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.common.checker.impl;

import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.common.checker.IInstalationUnitChecker;
import org.eclipse.cbi.p2repo.analyzers.common.utils.IUUtil;

/**
 * @author dhuebner - Initial contribution and API
 */
public class IUNameChecker implements IInstalationUnitChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu) {
		CheckReport report = createReport(iu);

		String iuName = iu.getProperty(IInstallableUnit.PROP_NAME, null);
		report.setCheckResult("Probably correct name");
		report.setAdditionalData(iuName);
		if (!IUUtil.isCategory(iu) && !IUUtil.isSpecial(iu) && !IUUtil.isFragment(iu)) {
			// not sure if can ever be null ... but, just in case
			if (iuName == null || (iuName.startsWith("%") || iuName.startsWith("Feature-")
					|| iuName.startsWith("Bundle-") || iuName.startsWith("feature") || iuName.startsWith("plugin")
					|| iuName.startsWith("Plugin.name") || iuName.startsWith("fragment.")
					|| iuName.startsWith("Eclipse.org") || iuName.startsWith("bundle"))) {
				report.setCheckResult("Missing or (probably) incorrect name");
				report.setType(ReportType.NOT_IN_TRAIN);
			}
		}
		consumer.accept(report);
	}

}
