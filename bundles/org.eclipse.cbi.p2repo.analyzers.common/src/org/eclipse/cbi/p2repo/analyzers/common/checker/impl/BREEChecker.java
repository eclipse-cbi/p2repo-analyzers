/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.common.checker.impl;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.common.checker.IArtifactChecker;
import org.eclipse.cbi.p2repo.analyzers.common.utils.CheckerUtils;
import org.eclipse.cbi.p2repo.analyzers.common.utils.IUUtil;
import org.osgi.framework.Constants;

/**
 * @author dhuebner - Initial contribution and API
 */
public class BREEChecker implements IArtifactChecker {
	private static final String EXCLUDE_PROPERTY = "breeExceptions";

	@Override
	public void check(final Consumer<? super CheckReport> consumer, final P2RepositoryDescription descr,
			final IInstallableUnit iu, IArtifactKey artKey, final File child) {
		CheckReport report = createReport(iu, artKey);
		String excludes = CheckerUtils.loadCheckerProperties(BREEChecker.class).getProperty(EXCLUDE_PROPERTY, null);
		if (excludes != null && excludes.contains(iu.getId())) {
			return;
		}
		try {
			@SuppressWarnings("deprecation")
			String bree = IUUtil.getBundleManifestEntry(child, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			boolean needsBree = needsBree(child);
			if ((bree != null) && !bree.isEmpty()) {
				// has BREE, confirm is java file
				if (needsBree) {
					report.setType(ReportType.INFO);
					report.setCheckResult(bree);
				} else {
					report.setType(ReportType.WARNING);
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

	private boolean needsBree(final File child) {
		return exportsPackages(child);
	}

	private boolean exportsPackages(final File child) {
		String entry = IUUtil.getBundleManifestEntry(child, Constants.EXPORT_PACKAGE);
		if (entry != null && !entry.isEmpty()) {
			return true;
		}
		return false;
	}
}
