/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.common.checker;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;

/**
 * @author dhuebner - Initial contribution and API
 */
public interface IChecker {
	default CheckReport createReport(IInstallableUnit iu) {
		return new CheckReport(this.getClass(), iu);
	}

}
