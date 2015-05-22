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

/**
 * @author dhuebner - Initial contribution and API
 */
public class VersionChecker implements IInstalationUnitChecker {

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
		int segmentCount = iu.getVersion().getSegmentCount();
		if (segmentCount != 4) {
			report.setType(ReportType.BAD_GUY);
			report.setCheckResult("Does not contain 4 parts version");
		}
		report.setAdditionalData(String.valueOf(segmentCount));
		consumer.accept(report);
	}

}
