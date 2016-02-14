package org.eclipse.cbi.p2repo.analyzers.common.checker;

import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;

public interface IInstalationUnitChecker extends IChecker {

	void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu);

}