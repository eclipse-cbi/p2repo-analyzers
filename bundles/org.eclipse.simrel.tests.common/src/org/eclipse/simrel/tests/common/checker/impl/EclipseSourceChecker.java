/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.checker.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.checker.IArtifactChecker;
import org.eclipse.simrel.tests.common.utils.IUUtil;

/**
 * @author dhuebner - Initial contribution and API
 */
public class EclipseSourceChecker implements IArtifactChecker {
	private static final String PROPERTY_ECLIPSE_SOURCEREFERENCES = "Eclipse-SourceReferences";

	@Override
	public void check(final Consumer<? super CheckReport> consumer, final P2RepositoryDescription descr,
			final IInstallableUnit iu, IArtifactKey artKey, final File child) {
		CheckReport report = createReport(iu, artKey);
		String es = IUUtil.getBundleManifestEntry(child, PROPERTY_ECLIPSE_SOURCEREFERENCES);
		String esExceptions = exceptions();
		if (!iu.getId().endsWith(".source")) {
			if (esExceptions.contains(iu.getId())) {
			} else {
				if ((es != null) && es.contains("project=")) {
					report.setCheckResult("Contains " + PROPERTY_ECLIPSE_SOURCEREFERENCES + " and project=");
				}
				if ((es != null) && (es.length() > 0)) {
					report.setCheckResult("Contains " + PROPERTY_ECLIPSE_SOURCEREFERENCES);
				} else {
					// no ES
					report.setCheckResult("Missing " + PROPERTY_ECLIPSE_SOURCEREFERENCES);
					report.setType(ReportType.BAD_GUY);
				}
			}
		}
		consumer.accept(report);
	}

	private String exceptions() {
		InputStream propertyStream = this.getClass().getResourceAsStream("exceptions.properties");
		Properties esExceptionProperties = new Properties();
		String esExceptions = "";
		try {
			esExceptionProperties.load(propertyStream);
			esExceptions = esExceptionProperties.getProperty("esExceptions");
			if (esExceptions == null) {
				esExceptions = "";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (propertyStream != null) {
				try {
					propertyStream.close();
				} catch (IOException e) {
					// would be unusual to get here?
					e.printStackTrace();
				}
			}
		}
		return esExceptions;
	}
}
