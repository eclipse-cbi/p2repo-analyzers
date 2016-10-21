/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.common.checker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.BREEChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.EclipseSourceChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.FeatureNameChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.IUNameChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.LayoutChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.LicenseConsistencyChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.ProviderNameChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.SignatureChecker;
import org.eclipse.cbi.p2repo.analyzers.common.checker.impl.VersionPatternChecker;

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
		this.checkers.add(new VersionPatternChecker());
		this.checkers.add(new IUNameChecker());
		this.checkers = Collections.unmodifiableSet(this.checkers);
		this.artifactCheckers.add(new BREEChecker());
		this.artifactCheckers.add(new SignatureChecker());
		this.artifactCheckers.add(new EclipseSourceChecker());
		this.artifactCheckers.add(new LayoutChecker());
		this.artifactCheckers = Collections.unmodifiableSet(this.artifactCheckers);
	}

	public Set<IInstalationUnitChecker> getCheckers() {
		return this.checkers;
	}

	public Set<IArtifactChecker> getArtifactCheckers() {
		return this.artifactCheckers;
	}
}
