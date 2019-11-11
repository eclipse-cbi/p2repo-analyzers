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
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.common.checker.IInstalationUnitChecker;
import org.eclipse.cbi.p2repo.analyzers.common.utils.IUUtil;

/**
 * @author dhuebner - Initial contribution and API
 */
public class VersionPatternChecker implements IInstalationUnitChecker {

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu) {
		CheckReport report = createReport(iu);
		Version version = iu.getVersion();
		if (!IUUtil.isSpecial(iu) && !IUUtil.isCategory(iu)) {
			if (version.isOSGiCompatible()) {
				if (version.getSegmentCount() == 4) {
					Comparable<?> qualifier = version.getSegment(3);
					if (!(qualifier instanceof String) || ((String) qualifier).isEmpty()) {
						report.setType(ReportType.BAD_GUY);
						report.setCheckResult("Empty qualifier segment.");
					}
				} else {
					report.setType(ReportType.BAD_GUY);
					report.setCheckResult("Does not contain 4 parts version");
				}
			} else {
				report.setType(ReportType.BAD_GUY);
				report.setCheckResult("Not an OSGI version");
			}
		}
		report.setAdditionalData(String.valueOf(version.getOriginal()));
		consumer.accept(report);
	}

}
