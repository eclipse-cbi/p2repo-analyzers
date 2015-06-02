package org.eclipse.simrel.tests.common.utils;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.stream.Stream;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryAnalyser;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.checker.CheckerRegistry;
import org.eclipse.simrel.tests.common.checker.IArtifactChecker;
import org.eclipse.simrel.tests.common.checker.IInstalationUnitChecker;
import org.eclipse.simrel.tests.common.checker.impl.FeatureNameChecker;
import org.eclipse.simrel.tests.common.checker.impl.LicenseConsistencyChecker;
import org.eclipse.simrel.tests.common.checker.impl.ProviderNameChecker;
import org.eclipse.simrel.tests.common.checker.impl.SignatureChecker;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRunner {

	// private static final int FEATURES_IN_REPO = 76;
	static final String JENKINS_XTEXT = "https://xtext-builds.itemis.de/jenkins/job/xtext-master/ws/xtext.p2.repository/";
	static final String EMF = "file:///Users/dhuebner/Downloads/emf-xsd-Update-N201505120526";
	static final String XTEXT = "file:///Users/dhuebner/Downloads/tmf-xtext-Update-2.8.3M7-2";
	static final String LUNA = "file:///Users/dhuebner/git/org.eclipse.xtext-master/releng/org.eclipse.xtext.releng/distrobuilder/luna/local-repo/final";
	private static CheckReportsManager reporter = null;

	@BeforeClass
	public static synchronized void setupOnce() throws ProvisionException, OperationCanceledException {
		if (reporter == null) {
			long start = System.currentTimeMillis();
			long time = start;

			P2RepositoryDescription p2Repo = IUUtil.createRepositoryDescription(URI.create(JENKINS_XTEXT));
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
		registry.getCheckers().forEach((final IInstalationUnitChecker element) -> System.out
				.println("   " + element.getClass().getSimpleName()));
		System.out.println("Artifact Checker:");
		registry.getArtifactCheckers().forEach(
				(final IArtifactChecker element) -> System.out.println("   " + element.getClass().getSimpleName()));
	}

	@Test
	public void testFeatureNames() {
		Stream<CheckReport> reportsByCheckerId = reportsByCheckerId(FeatureNameChecker.class.getName());
		assertEquals("To Long Featurenames", 0,
				reportsByCheckerId.filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}

	@Test
	public void testProviderNames() {
		assertEquals("Bad Provider name", 0, reportsByCheckerId(ProviderNameChecker.class.getName())
				.filter(report -> report.getType() == ReportType.BAD_GUY).count());
		assertEquals("Wrong Provider name", 0, reportsByCheckerId(ProviderNameChecker.class.getName())
				.filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
	}

	@Test
	public void testLicense() {
		Stream<CheckReport> filter = reportsByCheckerId(LicenseConsistencyChecker.class.getName())
				.filter(report -> report.getType() == ReportType.BAD_GUY);
		assertEquals("Old License", 0, filter.count());
	}

	@Test
	public void testSigning() {
		Stream<CheckReport> reportsByCheckerId = reportsByCheckerId(SignatureChecker.class.getName());
		Stream<CheckReport> filter = reportsByCheckerId.filter(report -> report.getType() == ReportType.NOT_IN_TRAIN);
		assertEquals("Not signed", 0, filter.count());
	}

	@Test
	public void testAllError() {
		assertEquals("Error Reports created", 0,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
		assertEquals("Warning Reports created", 0,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}

	private Stream<CheckReport> reportsByCheckerId(final String checkerId) {
		Stream<CheckReport> stream = reporter.getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(checkerId));
		return featureReports;
	}
}
