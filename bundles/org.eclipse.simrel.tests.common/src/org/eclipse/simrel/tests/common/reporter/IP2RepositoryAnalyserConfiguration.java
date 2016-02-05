/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.simrel.tests.common.reporter;

import java.net.URI;

/**
 * @author dhuebner - Initial contribution and API
 */
public interface IP2RepositoryAnalyserConfiguration {

	/**
	 * @return path to the output directory
	 */
	public abstract String getReportOutputDir();

	/**
	 * @return p2 repository location as {@link URI}
	 */
	public abstract URI getReportRepoURI();

	public abstract String getDataOutputDir();

}
