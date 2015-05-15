/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.checker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author dhuebner - Initial contribution and API
 */
public class CheckerRegistry {
	private Set<IInstalationUnitChecker> checkers = new HashSet<>();
	private Set<IArtifactChecker> artifactCheckers = new HashSet<>();

	public CheckerRegistry() {
		this.checkers.add(new FeatureNameChecker());
		this.checkers.add(new LicenseConsistencyChecker());
		this.checkers.add(new ProviderNameChecker());
		this.checkers = Collections.unmodifiableSet(this.checkers);
		this.artifactCheckers.add(new BREEChecker());
		this.artifactCheckers.add(new SignatureChecker());
		this.artifactCheckers = Collections.unmodifiableSet(this.artifactCheckers);
	}

	public Set<IInstalationUnitChecker> getCheckers() {
		return this.checkers;
	}

	public Set<IArtifactChecker> getArtifactCheckers() {
		return this.artifactCheckers;
	}
}
