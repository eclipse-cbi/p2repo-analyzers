package org.eclipse.simrel.tests.common.checker;

import java.util.function.Consumer;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;

public interface IInstalationUnitChecker {

	void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu);

}