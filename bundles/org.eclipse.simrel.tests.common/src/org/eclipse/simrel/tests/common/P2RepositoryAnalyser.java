/**
 * 
 */
package org.eclipse.simrel.tests.common;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.simrel.tests.common.checker.CheckerRegistry;

/**
 * @author dhuebner
 *
 */
public class P2RepositoryAnalyser {

	public void analyse(P2RepositoryDescription repoDescr, CheckerRegistry registry,
			Consumer<? super CheckReport> consumer) {
		analyse(repoDescr, registry, consumer, false);
	}

	public void analyse(final P2RepositoryDescription repoDescr, final CheckerRegistry registry,
			final Consumer<? super CheckReport> consumer, boolean split) {

		IQueryResult<IInstallableUnit> iQueryResult = collectInstalableUnits(repoDescr);
		Stream<IInstallableUnit> parallelStream;
		if (split) {
			parallelStream = StreamSupport.stream(iQueryResult.spliterator(), true);
		} else {
			Set<IInstallableUnit> set = iQueryResult.toUnmodifiableSet();
			parallelStream = set.parallelStream();
		}
		parallelStream.forEach(iu -> {
			registry.getCheckers().stream().forEach(checker -> checker.check(consumer, repoDescr, iu));
			iu.getArtifacts()
					.parallelStream()
					.map(key -> ((IFileArtifactRepository) repoDescr.getArtifactRepository()).getArtifactFile(key))
					.forEach(
							artifact -> registry.getArtifactCheckers().stream()
									.forEach(artChecker -> artChecker.check(consumer, repoDescr, iu, artifact)));
		});
	}

	protected IQueryResult<IInstallableUnit> collectInstalableUnits(P2RepositoryDescription descr) {
		return descr.getMetadataRepository().query(QueryUtil.createIUAnyQuery(), null);
	}

}
