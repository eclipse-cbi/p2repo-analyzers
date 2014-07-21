/**
 * 
 */
package org.eclipse.simrel.tests.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IFileArtifactRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.simrel.tests.common.Activator;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.osgi.framework.BundleException;

/**
 * @author dhuebner
 *
 */
public class IUUtil {
	private static final String PROPERTY_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT = "Bundle-RequiredExecutionEnvironment";

	public static boolean isFeature(IInstallableUnit iu) {
		return iu
				.getArtifacts()
				.stream()
				.anyMatch(
						art -> "org.eclipse.update.feature".equals(art
								.getClassifier()));
	}

	public static P2RepositoryDescription createRepositoryDescription(
			URI p2RepoURL) throws ProvisionException,
			OperationCanceledException {
		P2RepositoryDescription description = new P2RepositoryDescription();
		description.setRepoURL(p2RepoURL);
		IMetadataRepository repo = Activator.getMetadataRepositoryManager()
				.loadRepository(p2RepoURL, null);
		if (repo == null) {
			handleFatalError("no metadata repository found at "
					+ p2RepoURL.toString());
		}
		description.setMetadataRepository(repo);
		IArtifactRepository artRepo = Activator.getArtifactRepositoryManager()
				.loadRepository(p2RepoURL, null);
		if (artRepo == null) {
			handleFatalError("no artifact repository found at "
					+ p2RepoURL.toString());
		}
		description.setArtifactRepository((IFileArtifactRepository) artRepo);
		return description;
	}

	private static void handleFatalError(String string) {
		System.err.println(string);
	}

	public static String getBREEFromJAR(File file) {
		InputStream input = null;
		JarFile jar = null;
		try {
			jar = new JarFile(file, false, ZipFile.OPEN_READ);
			JarEntry entry = jar.getJarEntry(JarFile.MANIFEST_NAME);
			if (entry == null) {
				// addError("Bundle does not contain a MANIFEST.MF file: " +
				// file.getAbsolutePath());
				return null;
			}
			input = jar.getInputStream(entry);
			return getBREEFromManifest(input, file.getAbsolutePath());
		} catch (IOException e) {
			System.out.println(e.getMessage());
			// addError(e.getMessage());
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
			if (jar != null) {
				try {
					jar.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public static boolean isSpecial(IInstallableUnit iu) {
		// TODO: I assume 'executable roots', etc. have no readable name?
		/*
		 * TODO: what are these special things? What ever they are, they have no
		 * provider name. config.a.jre is identified as a fragment
		 * (org.eclipse.equinox.p2.type.fragment). a.jre has no properties.
		 */
		String iuId = iu.getId();
		boolean isSpecial = iuId.startsWith("a.jre")
				|| iuId.startsWith("config.a.jre") || iuId.endsWith("_root")
				|| iuId.contains(".executable.")
				|| iuId.contains("configuration_root")
				|| iuId.contains("executable_root")
				|| iuId.startsWith("toolingorg.eclipse")
				|| iuId.startsWith("tooling.");
		return isSpecial;
	}

	/*
	 * Return the bundle id from the manifest pointed to by the given input
	 * stream.
	 */
	private static String getBREEFromManifest(InputStream input, String path) {
		String bree = null;
		try {
			Map<String, String> attributes = ManifestElement
					.parseBundleManifest(input, null);
			bree = (String) attributes
					.get(PROPERTY_BUNDLE_REQUIRED_EXECUTION_ENVIRONMENT);
		} catch (BundleException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		return bree;
	}
}
