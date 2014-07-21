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
import org.eclipse.simrel.tests.common.checker.LicenseConsistencyChecker;
import org.eclipse.simrel.tests.common.checker.ReportType;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRunner {

	private static final int FEATURES_IN_REPO = 76;
	private static final String XTEXT = "/Users/dhuebner/hudsonbuild/tmf-xtext-head/buildroot/buckminster.workspace/output/org.eclipse.xtext.build_2.6.0-eclipse.feature/site.p2";
	private static final String KEPLER = "file:///Users/dhuebner/git/xtext-master/releng/org.eclipse.xtext.releng/distrobuilder/kepler/local-repo/final";
	private static CheckReportsManager reporter = null;

	@BeforeClass
	public static void setupOnce() throws ProvisionException, OperationCanceledException {
		if (reporter == null) {
			P2RepositoryAnalyser analyser = new P2RepositoryAnalyser();
			reporter = new CheckReportsManager();
			long start = System.currentTimeMillis();
			long time = start;
			P2RepositoryDescription p2Repo = IUUtil.createRepositoryDescription(URI.create(KEPLER));
			System.out.println("repo descr " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();
			CheckerRegistry registry = new CheckerRegistry();
			analyser.analyse(p2Repo, registry, reporter);
			System.out.println("analyse " + (System.currentTimeMillis() - time) + "ms");
			time = System.currentTimeMillis();
			reporter.dumpReports();
			System.out.println("dump " + (System.currentTimeMillis() - time) + "ms");
			System.out.println("all " + (System.currentTimeMillis() - start) + "ms");
		}
	}

	@Test
	public void testFeatureNames() {
		Stream<CheckReport> stream = reporter.getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(
				FeatureNameChecker.class.getName()));
		assertEquals("To Long Featurenames", 0, featureReports.filter(report -> report.getType() == ReportType.BAD_GUY)
				.count());
	}

	@Test
	public void testLicense() {
		Stream<CheckReport> stream = reporter.getReports().parallelStream();
		Stream<CheckReport> featureReports = stream.filter(report -> report.getCheckerId().equals(
				LicenseConsistencyChecker.class.getName()));
		Stream<CheckReport> filter = featureReports.filter(report -> report.getType() == ReportType.BAD_GUY);
		ArrayList<CheckReport> collect = filter.collect(Collectors.toCollection(ArrayList::new));
		assertEquals("Old License", 3, collect.size());
	}

	@Test
	public void testAllError() {
		assertEquals("Error Reports created", 0,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.NOT_IN_TRAIN).count());
		assertEquals("Warning Reports created", 6,
				reporter.getReports().stream().filter(report -> report.getType() == ReportType.BAD_GUY).count());
	}
}
