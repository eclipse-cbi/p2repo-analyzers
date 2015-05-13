/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.simrel.tests.common.checker;

import java.io.File;
import java.util.function.Consumer;
import java.util.jar.JarEntry;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.utils.IUUtil;

/**
 * @author dhuebner - Initial contribution and API
 */
public class SignatureChecker implements IArtifactChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu,
			IArtifactKey artifactKey, File file) {

		CheckReport report = createReport(iu);
		JarEntry jarEntry = IUUtil.getJarEntry(file, "META-INF/ECLIPSE_.RSA");
		if (jarEntry == null) {
			if (!artifactKey.getClassifier().equals("binary")) {
				report.setCheckResult("Jar is probably not signed.");
				report.setType(ReportType.NOT_IN_TRAIN);
			} else {
				report.setCheckResult("Unsigned binary file.");
				report.setType(ReportType.INFO);
			}
		}
		consumer.accept(report);
	}

}
