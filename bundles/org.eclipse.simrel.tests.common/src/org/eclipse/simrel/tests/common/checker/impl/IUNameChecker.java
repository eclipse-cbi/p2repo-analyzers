/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
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
public class IUNameChecker implements IInstalationUnitChecker {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker#check(
	 * java.util.function.Consumer,
	 * org.eclipse.simrel.tests.common.P2RepositoryDescription,
	 * org.eclipse.equinox.p2.metadata.IInstallableUnit)
	 */
	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu) {
		CheckReport report = createReport(iu);
		// ignore categories
		boolean isCategory = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.category"));
		// TODO: should we exclude fragments?
		boolean isFragment = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.fragment"));

		if (!isCategory && !IUUtil.isSpecial(iu) && !isFragment) {
			String bundleName = iu.getProperty(IInstallableUnit.PROP_NAME, null);
			// not sure if can ever be null ... but, just in case
			if (bundleName == null || (bundleName.startsWith("%") || bundleName.startsWith("Feature-")
					|| bundleName.startsWith("Bundle-") || bundleName.startsWith("feature")
					|| bundleName.startsWith("plugin") || bundleName.startsWith("Plugin.name")
					|| bundleName.startsWith("fragment.") || bundleName.startsWith("Eclipse.org")
					|| bundleName.startsWith("bundle"))) {
				report.setCheckResult("Missing or (probably) incorrect name");
				report.setType(ReportType.NOT_IN_TRAIN);
			}
			report.setAdditionalData(bundleName);
		}
		consumer.accept(report);
	}

}
