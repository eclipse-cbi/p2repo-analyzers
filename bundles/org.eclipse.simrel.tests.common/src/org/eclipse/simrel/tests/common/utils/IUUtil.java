/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.simrel.tests.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
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
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.internal.Activator;
import org.osgi.framework.BundleException;

public class IUUtil {

	public static boolean isFeature(final IInstallableUnit iu) {
		return iu.getArtifacts().stream().anyMatch(art -> "org.eclipse.update.feature".equals(art.getClassifier()));
	}

	public static P2RepositoryDescription createRepositoryDescription(final URI p2RepoURL)
			throws ProvisionException, OperationCanceledException {
		P2RepositoryDescription description = new P2RepositoryDescription();
		description.setRepoURL(p2RepoURL);
		IMetadataRepository repo = Activator.getMetadataRepositoryManager().loadRepository(p2RepoURL, null);
		if (repo == null) {
			handleFatalError("no metadata repository found at " + p2RepoURL.toString());
		}
		description.setMetadataRepository(repo);
		IArtifactRepository artRepo = Activator.getArtifactRepositoryManager().loadRepository(p2RepoURL, null);
		if (artRepo == null) {
			handleFatalError("no artifact repository found at " + p2RepoURL.toString());
		}
		description.setArtifactRepository((IFileArtifactRepository) artRepo);
		return description;
	}

	private static void handleFatalError(final String string) {
		System.err.println(string);
	}

	public static boolean isSpecial(final IInstallableUnit iu) {
		// TODO: I assume 'executable roots', etc. have no readable name?
		/*
		 * TODO: what are these special things? What ever they are, they have no
		 * provider name. config.a.jre is identified as a fragment
		 * (org.eclipse.equinox.p2.type.fragment). a.jre has no properties.
		 */
		String iuId = iu.getId();
		boolean isSpecial = iuId.startsWith("a.jre") || iuId.startsWith("config.a.jre") || iuId.endsWith("_root")
				|| iuId.contains(".executable.") || iuId.contains("configuration_root")
				|| iuId.contains("executable_root") || iuId.startsWith("toolingorg.eclipse")
				|| iuId.startsWith("tooling.");
		return isSpecial;
	}

	/*
	 * The given file points to a bundle contained in an archive. Look into the
	 * bundle manifest file to find the bundle identifier.
	 */
	public static String getBundleManifestEntry(final File jarfile, final String key) {
		return workWithJarEntry(jarfile, JarFile.MANIFEST_NAME, (jarEntry, stream) -> {
			String result = null;
			if (jarEntry != null) {
				try {
					Map<String, String> attributes = ManifestElement.parseBundleManifest(stream, null);
					result = attributes.get(key);
				} catch (BundleException | IOException e) {
					throw new RuntimeException(e);
				}
			}
			return result;
		});

	}

	public static <T> T workWithJarEntry(final File file, final String entryName,
			BiFunction<JarEntry, InputStream, T> function) {
		JarFile jar = null;
		InputStream entryStream = null;
		T result = null;
		try {
			jar = new JarFile(file, false, ZipFile.OPEN_READ);
			JarEntry jarEntry = jar.getJarEntry(entryName);
			if (jarEntry != null) {
				entryStream = jar.getInputStream(jarEntry);
			}
			result = function.apply(jarEntry, entryStream);
		} catch (IOException e) {
			handleFatalError(e.getMessage());
		} finally {
			if (entryStream != null) {
				try {
					entryStream.close();
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
		return result;
	}

	public static Properties getEclipseInf(File jarfile) {
		return workWithJarEntry(jarfile, "META-INF/eclipse.inf", (jarEntry, entryStream) -> {
			Properties properties = new Properties();
			if (jarEntry != null) {
				try {
					properties.load(entryStream);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return properties;
		});
	}

	/**
	 * @param file
	 *            jarFile
	 * @param entryName
	 *            Jar entry name
	 * @return {@link JarEntry} or <code>null</code> if not exists
	 */
	public static JarEntry getJarEntry(File file, String entryName) {
		return workWithJarEntry(file, entryName, (jarEntry, entryStream) -> jarEntry);
	}

	public static String versionedId(final IInstallableUnit iu) {
		return iu.getId() + (iu.getVersion() != null ? "_" + iu.getVersion().getOriginal() : "");
	}
}
