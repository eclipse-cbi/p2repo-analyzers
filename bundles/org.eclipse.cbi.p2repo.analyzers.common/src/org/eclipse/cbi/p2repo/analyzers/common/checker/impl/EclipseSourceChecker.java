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

/**
 * @author dhuebner - Initial contribution and API
 */
public class EclipseSourceChecker implements IArtifactChecker {
	private static final String ES_EXCEPTIONS_PROPERTY = "esExceptions";
	private static final String PROPERTY_ECLIPSE_SOURCEREFERENCES = "Eclipse-SourceReferences";
	private String excludes;

	@Override
	public void check(final Consumer<? super CheckReport> consumer, final P2RepositoryDescription descr,
			final IInstallableUnit iu, IArtifactKey artKey, final File child) {
		CheckReport report = createReport(iu, artKey);
		String es = IUUtil.getBundleManifestEntry(child, PROPERTY_ECLIPSE_SOURCEREFERENCES);
		if (!iu.getId().endsWith(".source")) {
			String excludedIus = getExcludes();
			if (!excludedIus.isEmpty() && excludedIus.contains(iu.getId())) {
				report.setCheckResult("Skipped");
			} else {
				if ((es != null) && es.contains("project=")) {
					report.setCheckResult("Contains " + PROPERTY_ECLIPSE_SOURCEREFERENCES + " and project=");
				}
				if ((es != null) && (!es.isEmpty())) {
					report.setCheckResult("Contains " + PROPERTY_ECLIPSE_SOURCEREFERENCES);
				} else {
					// no ES
					report.setCheckResult("Missing " + PROPERTY_ECLIPSE_SOURCEREFERENCES);
					report.setType(ReportType.WARNING);
				}
			}
		}
		consumer.accept(report);
	}

	private String getExcludes() {
		if (this.excludes == null) {
			this.excludes = CheckerUtils.loadCheckerProperties(EclipseSourceChecker.class)
					.getProperty(ES_EXCEPTIONS_PROPERTY, "");
		}
		return this.excludes;
	}

}
