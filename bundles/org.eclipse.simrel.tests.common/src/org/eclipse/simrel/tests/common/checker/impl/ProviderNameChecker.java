/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.checker.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker;
import org.eclipse.simrel.tests.common.utils.CheckerUtils;
import org.eclipse.simrel.tests.common.utils.IUUtil;

/**
 * @author Dennis Huebner
 *
 *
 */
public class ProviderNameChecker implements IInstalationUnitChecker {

	private static final String OLD_PROVIDER_NAME = "Eclipse.org";

	private Set<String> knownProviderNames = null;

	@Override
	public void check(final Consumer<? super CheckReport> consumer, final P2RepositoryDescription descr,
			final IInstallableUnit iu) {

		// || iu.getId().endsWith("feature.group")
		if (!IUUtil.isCategory(iu) && !IUUtil.isSpecial(iu) && !IUUtil.isFragment(iu)) {
			CheckReport checkReport = new CheckReport(ProviderNameChecker.class, iu);

			String providerName = iu.getProperty(IInstallableUnit.PROP_PROVIDER, null);
			checkReport.setCheckResult(providerName);

			if (providerName == null) {
				missingProviderName(checkReport);
			} else if (providerName.startsWith("%") || providerName.equals("Eclipse")
					|| providerName.startsWith("eclipse.org") || providerName.equals("unknown")
					|| providerName.startsWith("Engineering") || providerName.contains("org.eclipse.jwt")
					|| providerName.contains("www.example.org") || providerName.contains("www.eclipse.org")
					|| providerName.contains("Provider") || providerName.contains("provider")
					|| providerName.startsWith("Bundle-") || providerName.startsWith("bund")
					|| providerName.startsWith("Eclispe")) {
				// common errors and misspellings
				incorrectProviderName(checkReport);
			} else if (providerName.startsWith("Eclipse.org - ")) {
				correctProviderName(checkReport);
			} else if (getKnownProviderNames().contains(providerName)) {
				correctProviderName(checkReport);
			} else if (OLD_PROVIDER_NAME.equals(providerName)) {
				oldProviderName(checkReport);
			} else if (providerName.startsWith("Eclipse")) {
				// order is important, starts with Eclipse, but not one
				// of the above e.g. "Eclipse Orbit" or "Eclipse.org"?
				// TODO: eventually put in with "incorrect?"
				unknownProviderName(checkReport);
			} else {
				if (iu.getId().startsWith("org.eclipse")) {
					suspectProviderName(checkReport);
				} else {
					unknownProviderName(checkReport);
				}
			}
			checkReport.setCheckResult(providerName);
			consumer.accept(checkReport);
		}

	}

	/**
	 * @param checkReport
	 */
	private void missingProviderName(CheckReport checkReport) {
		checkReport.setType(ReportType.NOT_IN_TRAIN);
		checkReport.setAdditionalData("Provider name is missing.");
	}

	public Set<String> getKnownProviderNames() {
		if (this.knownProviderNames == null) {
			Set<String> temp = new HashSet<>();
			String expectedProviderNames = CheckerUtils.loadCheckerProperties(ProviderNameChecker.class)
					.getProperty("expectedProviderNames", "");
			if (!expectedProviderNames.isEmpty()) {
				String[] names = expectedProviderNames.split(",");
				for (String string : names) {
					temp.add(string);
				}
			}
			this.knownProviderNames = Collections.unmodifiableSet(temp);
		}
		return this.knownProviderNames;
	}

	private void suspectProviderName(final CheckReport checkReport) {
		checkReport.setType(ReportType.NOT_IN_TRAIN);
		checkReport.setAdditionalData("Suspect provider name.");
	}

	private void incorrectProviderName(final CheckReport checkReport) {
		checkReport.setType(ReportType.NOT_IN_TRAIN);
		checkReport.setAdditionalData("Incorrect provider name.");
	}

	private void unknownProviderName(final CheckReport checkReport) {
		checkReport.setType(ReportType.BAD_GUY);
		checkReport.setAdditionalData("Unknown provider name.");
	}

	private void oldProviderName(final CheckReport checkReport) {
		checkReport.setType(ReportType.WARNING);
		checkReport.setAdditionalData("Old provider name.");
	}

	private void correctProviderName(final CheckReport checkReport) {
		checkReport.setType(ReportType.INFO);
		checkReport.setAdditionalData("Correct provider name.");
	}
}
