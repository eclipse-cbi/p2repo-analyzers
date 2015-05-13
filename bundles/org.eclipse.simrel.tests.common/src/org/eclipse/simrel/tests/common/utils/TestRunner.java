package org.eclipse.simrel.tests.common.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryAnalyser;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.checker.CheckerRegistry;
import org.eclipse.simrel.tests.common.checker.FeatureNameChecker;
import org.eclipse.simrel.tests.common.checker.IArtifactChecker;
import org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker;
import org.eclipse.simrel.tests.common.checker.LicenseConsistencyChecker;
import org.eclipse.simrel.tests.common.checker.ProviderNameChecker;
import org.eclipse.simrel.tests.common.checker.SignatureChecker;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRunner {

	// private static final int FEATURES_IN_REPO = 76;
	static final String EMF = "file:///Users/dhuebner/Downloads/emf-xsd-Update-N201505120526";
	static final String XTEXT = "file:///Users/dhuebner/Downloads/tmf-xtext-Update-2.8.3M7-2";
	static final String LUNA = "file:///Users/dhuebner/git/org.eclipse.xtext-master/releng/org.eclipse.xtext.releng/distrobuilder/luna/local-repo/final";
	private static CheckReportsManager reporter = null;

	@BeforeClass
	public static void setupOnce() throws ProvisionException, OperationCanceledException {
		if (reporter == null) {
			long start = System.currentTimeMillis();
			long time = start;

			P2RepositoryDescription p2Repo = IUUtil.createRepositoryDescription(URI.create(EMF));
			System.out.println("create repo descr " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();

			CheckerRegistry registry = new CheckerRegistry();
			dumpCheckerRegistry(registry);

			P2RepositoryAnalyser analyser = new P2RepositoryAnalyser();
			reporter = new CheckReportsManager();
			analyser.analyse(p2Repo, registry, reporter);
			System.out.println("run analyse " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();

			reporter.dumpReports();
			System.out.println("do dump " + (System.currentTimeMillis() - time) + "ms");
			System.out.println("overall " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	private static void dumpCheckerRegistry(CheckerRegistry registry) {
		System.out.println("IU Checker:");
		registry.getCheckers().forEach(
				(final IInstalationUnitChecker element) -> System.out.println("   "+element.getClass().getSimpleName()));
		System.out.println("Artifact Checker:");
		registry.getArtifactCheckers()
				.forEach((final IArtifactChecker element) -> System.out.println("   "+element.getClass().getSimpleName()));
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
		assertEquals("Wrong Provider name", 1,
				reports.filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
	}

	@Test
	public void testLicense() {
		Stream<CheckReport> filter = reportsByCheckerId(LicenseConsistencyChecker.class.getName())
				.filter(report -> report.getType() == ReportType.BAD_GUY);
		ArrayList<CheckReport> collect = filter.collect(Collectors.toCollection(ArrayList::new));
		assertEquals("Old License", 1, collect.size());
	}

	@Test
	public void testSigning() {
		Stream<CheckReport> filter = reportsByCheckerId(SignatureChecker.class.getName())
				.filter(report -> report.getType() == ReportType.NOT_IN_TRAIN);
		ArrayList<CheckReport> collect = filter.collect(Collectors.toCollection(ArrayList::new));
		assertEquals("Not signed", 3, collect.size());
	}

	@Test
	public void testAllError() {
		assertEquals("Error Reports created", 4,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
		assertEquals("Warning Reports created", 8,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}

	private Stream<CheckReport> reportsByCheckerId(final String checkerId) {
		Stream<CheckReport> stream = reporter.getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(checkerId));
		return featureReports;
	}
}
