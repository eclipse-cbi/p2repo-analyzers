/**
 * 
 */
package org.eclipse.simrel.tests.common.checker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author dhuebner
 *
 */
public class CheckerRegistry {
	private Set<IUChecker> checkers = new HashSet<>();
	private Set<IArtifactChecker> artifactCheckers = new HashSet<>();

	public CheckerRegistry() {
		checkers.add(new FeatureNameChecker());
		checkers.add(new LicenseConsistencyChecker());
		checkers = Collections.unmodifiableSet(checkers);
		artifactCheckers.add(new BREEChecker());
		artifactCheckers = Collections.unmodifiableSet(artifactCheckers);
	}

	public Set<IUChecker> getCheckers() {
		return checkers;
	}

	public Set<IArtifactChecker> getArtifactCheckers() {
		return artifactCheckers;
	}
}
