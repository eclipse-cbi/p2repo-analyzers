package org.eclipse.simrel.tests.common.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.CheckReportsManager;
import org.eclipse.simrel.tests.common.P2RepositoryAnalyser;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.checker.CheckerRegistry;
import org.eclipse.simrel.tests.common.checker.FeatureNameChecker;
import org.eclipse.simrel.tests.common.checker.IArtifactChecker;
import org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker;
import org.eclipse.simrel.tests.common.checker.LicenseConsistencyChecker;
import org.eclipse.simrel.tests.common.checker.ProviderNameChecker;
import org.eclipse.simrel.tests.common.checker.ReportType;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRunner {

	private static final int FEATURES_IN_REPO = 76;
	private static final String XTEXT = "file:///Users/dhuebner/Desktop/xtext.p2.repository/";
	private static final String LUNA = "file:///Users/dhuebner/git/xtext-master/releng/org.eclipse.xtext.releng/distrobuilder/luna/local-repo/final";
	private static CheckReportsManager reporter = null;

	@BeforeClass
	public static void setupOnce() throws ProvisionException, OperationCanceledException {
		if (reporter == null) {
			P2RepositoryAnalyser analyser = new P2RepositoryAnalyser();
			reporter = new CheckReportsManager();

			long start = System.currentTimeMillis();
			long time = start;

			P2RepositoryDescription p2Repo = IUUtil.createRepositoryDescription(URI.create(LUNA));
			System.out.println("create repo descr " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();

			CheckerRegistry registry = new CheckerRegistry();
			System.out.println("IU Checker:");
			registry.getCheckers().forEach(
					(final IInstalationUnitChecker element) -> System.out.println(element.getClass().getSimpleName()));
			System.out.println("Artifact Checker:");
			registry.getArtifactCheckers().forEach(
					(final IArtifactChecker element) -> System.out.println(element.getClass().getSimpleName()));

			analyser.analyse(p2Repo, registry, reporter);
			System.out.println("run analyse " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();

			reporter.dumpReports();
			System.out.println("do dump " + (System.currentTimeMillis() - time) + "ms");
			System.out.println("overall " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	@Test
	public void testFeatureNames() {
		Stream<CheckReport> reportsByCheckerId = reportsByCheckerId(FeatureNameChecker.class.getName());
		assertEquals("To Long Featurenames", 0,
				reportsByCheckerId.filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}

	@Test
	public void testProviderNames() {
		Stream<CheckReport> reports = reportsByCheckerId(ProviderNameChecker.class.getName());
		assertEquals("Wrong Provider name", 1, reports.filter(report -> report.getType() == ReportType.NOT_IN_TRAIN)
				.count());
	}

	@Test
	public void testLicense() {
		Stream<CheckReport> filter = reportsByCheckerId(LicenseConsistencyChecker.class.getName()).filter(
				report -> report.getType() == ReportType.BAD_GUY);
		ArrayList<CheckReport> collect = filter.collect(Collectors.toCollection(ArrayList::new));
		assertEquals("Old License", 1, collect.size());
	}

	@Test
	public void testAllError() {
		assertEquals("Error Reports created", 1,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
		assertEquals("Warning Reports created", 13,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}

	private Stream<CheckReport> reportsByCheckerId(final String checkerId) {
		Stream<CheckReport> stream = reporter.getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(checkerId));
		return featureReports;
	}
}
