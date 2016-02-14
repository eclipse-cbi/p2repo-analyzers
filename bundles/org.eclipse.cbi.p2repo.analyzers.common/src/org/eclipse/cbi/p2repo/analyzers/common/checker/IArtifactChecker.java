package org.eclipse.cbi.p2repo.analyzers.common.checker;

import java.io.File;
import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;

public interface IArtifactChecker extends IChecker {
	void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu,
			IArtifactKey artifact, File file);

	default CheckReport createReport(IInstallableUnit iu, IArtifactKey artifactKey) {
		return new CheckReport(this.getClass(), iu, artifactKey);
	}
}
