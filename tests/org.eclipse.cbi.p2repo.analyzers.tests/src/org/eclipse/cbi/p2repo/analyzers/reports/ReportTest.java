package org.eclipse.cbi.p2repo.analyzers.reports;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.cbi.p2repo.analyzers.BuildRepoTests;
import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.core.runtime.FileLocator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReportTest {

	static Path data;

	@BeforeClass
	public static void download() {
		try {
			URL self = FileLocator.resolve(new URI("platform:/plugin/org.eclipse.cbi.p2repo.analyzers.tests").toURL());
			data = "file".equals(self.getProtocol()) ? Path.of(self.toURI()).resolve("data")
					: Files.createTempDirectory("report-test");

			URL sampleSite = new URI("https://download.eclipse.org/oomph/updates/latest/org.eclipse.oomph.site.zip").toURL();
			try (ZipInputStream in = new ZipInputStream(sampleSite.openStream())) {
				ZipEntry entry;
				while ((entry = in.getNextEntry()) != null) {
					if (!entry.isDirectory()) {
						String name = entry.getName();
						Path target = data.resolve(name);
						if (!Files.exists(target)) {
							if (!Files.isDirectory(target.getParent())) {
								Files.createDirectories(target.getParent());
							}
							Files.copy(in, target);
						}
					}
				}
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testReportGenerator() {
		RepoTestsConfiguration configuration = new RepoTestsConfiguration(data.toString(),
				data.resolve("report").toString(), data.toString(), null);
		new BuildRepoTests(configuration).execute();
	}
}
