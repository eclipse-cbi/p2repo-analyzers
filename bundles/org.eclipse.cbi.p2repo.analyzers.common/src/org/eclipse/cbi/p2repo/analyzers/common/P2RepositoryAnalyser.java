/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.common;

import java.io.File;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.cbi.p2repo.analyzers.common.checker.CheckerRegistry;
import org.eclipse.cbi.p2repo.analyzers.common.checker.IInstalationUnitChecker;

/**
 * @author dhuebner - Initial contribution and API
 */
public class P2RepositoryAnalyser {

	public void analyse(final P2RepositoryDescription repoDescr, final CheckerRegistry registry,
			final Consumer<? super CheckReport> consumer) {
		analyse(repoDescr, registry, consumer, false);
	}

	public void analyse(final P2RepositoryDescription repoDescr, final CheckerRegistry registry,
			final Consumer<? super CheckReport> consumer, final boolean split) {

		IQueryResult<IInstallableUnit> iQueryResult = collectInstalableUnits(repoDescr);
		Stream<IInstallableUnit> parallelStream;
		if (split) {
			parallelStream = StreamSupport.stream(iQueryResult.spliterator(), true);
		} else {
			Set<IInstallableUnit> set = iQueryResult.toUnmodifiableSet();
			parallelStream = set.parallelStream();
		}
		parallelStream.forEach(iu -> {
			// run content unit tests
			registry.getCheckers().stream().forEach(checker -> checker.check(consumer, repoDescr, iu));
			// run artifacts tests for each artifact which belongs to IU and has
			// a File loadable
			iu.getArtifacts().parallelStream().forEach(artifactKey -> {
				File artifactFile = repoDescr.getArtifactRepository().getArtifactFile(artifactKey);
				if (artifactFile != null) {
					registry.getArtifactCheckers().stream().forEach(artChecker -> {
						artChecker.check(consumer, repoDescr, iu, artifactKey, artifactFile);

					});
				} else {
					CheckReport report = new CheckReport(SystemCheck.class, iu, artifactKey);
					report.setType(ReportType.INFO);
					report.setCheckResult("Unable to load artifact file.");
					report.setAdditionalData(
							artifactKey.toExternalForm() + " from " + repoDescr.getArtifactRepository().getLocation());
					consumer.accept(report);
				}

			});
		});
	}

	protected IQueryResult<IInstallableUnit> collectInstalableUnits(final P2RepositoryDescription descr) {
		return descr.getMetadataRepository().query(QueryUtil.createIUAnyQuery(), null);
	}

	class SystemCheck implements IInstalationUnitChecker {

		@Override
		public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu) {

		}
	}
}
