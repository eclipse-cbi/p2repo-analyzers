/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.common.checker.impl;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.P2RepositoryDescription;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.common.checker.IArtifactChecker;
import org.eclipse.cbi.p2repo.analyzers.common.utils.CheckerUtils;
import org.eclipse.cbi.p2repo.analyzers.common.utils.IUUtil;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @author dhuebner - Initial contribution and API
 */
public class LayoutChecker implements IArtifactChecker {
	private static final String KEY_DFT_BIN_JAR = "default.binary.jar";
	private static final String KEY_DFT_SRC_JAR = "default.source.jar";
	private static final String KEY_DFT_FEATURE = "default.feature";

	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit iu,
			IArtifactKey artifactKey, File file) {
		CheckReport report = createReport(iu, artifactKey);
		Properties checkerProperties = CheckerUtils.loadCheckerProperties(LayoutChecker.class);
		String property;
		boolean sourceIU = isSourceIUName(iu.getId());
		if (IUUtil.isFeature(iu)) {
			property = KEY_DFT_FEATURE;
		} else if (sourceIU) {
			property = KEY_DFT_SRC_JAR;
		} else {
			property = KEY_DFT_BIN_JAR;
		}

		String expectedEntries = checkerProperties.getProperty(property, null);
		if (sourceIU) {
			expectedEntries = MessageFormat.format(expectedEntries, iu.getId().substring(0, iu.getId().lastIndexOf('.')));
		} else {
			expectedEntries = MessageFormat.format(expectedEntries, iu.getId());
		}
		processArchive(file, Arrays.asList(expectedEntries.split(",")), report);

		consumer.accept(report);
	}

	private boolean isSourceIUName(String id) {
		return id.endsWith(".source") || id.endsWith(".infopop") || id.endsWith(".doc.user") || id.endsWith(".doc")
				|| id.endsWith(".doc.isv") || id.endsWith(".doc.dev") || id.endsWith(".doc.api") || id.endsWith("standard.schemas")
				|| id.endsWith(".branding");
	}

	private void processArchive(File file, List<String> expected, CheckReport report) {
		try (ZipFile zip = new ZipFile(file, ZipFile.OPEN_READ);){
			Map<String, String> found = new HashMap<>();
			for (String string : expected) {
				found.put(string, null);
			}
			for (Enumeration<?> e = zip.entries(); e.hasMoreElements();) {
				if (!found.values().contains(null)) {
					break;
				}
				ZipEntry entry = (ZipEntry) e.nextElement();
				String name = entry.getName();
				for (String expectedEntry : found.keySet()) {
					try {
						if (name.matches(expectedEntry.trim())) {
							found.put(expectedEntry, name);
						}
					} catch (PatternSyntaxException ex) {
						ex.printStackTrace();
					}
				}
			}
			String missing = found.entrySet().stream().filter(entry -> entry.getValue() == null).map(entry -> entry.getKey())
					.collect(Collectors.joining(", "));
			if (!missing.isEmpty()) {
				report.setType(ReportType.NOT_IN_TRAIN);
				report.setCheckResult("Missing " + missing + " in file: " + file.getName());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
