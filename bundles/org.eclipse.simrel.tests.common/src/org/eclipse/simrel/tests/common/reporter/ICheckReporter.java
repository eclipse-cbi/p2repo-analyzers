/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.simrel.tests.common.reporter;

import org.eclipse.simrel.tests.common.CheckReport;

/**
 * @author dhuebner - Initial contribution and API
 */
public interface ICheckReporter {
	void dumpReport(CheckReport report);
}
