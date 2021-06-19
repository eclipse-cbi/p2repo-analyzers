/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation

 ******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.FullJarNameParser;
import org.eclipse.cbi.p2repo.analyzers.utils.JARFileNameFilter;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

/**
 * Test to check MANIFEST.MF files in jars in a repository for presence of
 * Eclipse-SourceReferences.
 *
 * @since 3.7
 */
public class ESTest extends TestJars {

    public ESTest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private static final Object            PROPERTY_ECLIPSE_SOURCEREFERENCES = "Eclipse-SourceReferences";
    private static final FullJarNameParser nameParser                        = new FullJarNameParser();
    private static final String            outputFilename                    = "esdata.txt";

    public static void main(String[] args) {

        ESTest testlayout = new ESTest(RepoTestsConfiguration.createFromSystemProperties());
        testlayout.setDirectoryToCheck("/home/files/buildzips/junoRC2/eclipseJEE/");
        try {
            testlayout.testESSettingRule();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean testESSettingRule() throws IOException {

        File inputdir = new File(getBundleDirectory());

        createReportWriter(outputFilename);
        return checkBundleES(inputdir);
    }

    private boolean checkBundleES(File inputdir) throws IOException {
        // reset/initialize errors
        InputStream propertyStream = this.getClass().getResourceAsStream("exceptions.properties");
        Properties esExceptionProperties = new Properties();
        String esExceptions = "";
        try {
            esExceptionProperties.load(propertyStream);
            esExceptions = esExceptionProperties.getProperty("esExceptions");
            if (esExceptions == null) {
                esExceptions = "";
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (propertyStream != null) {
                try {
                    propertyStream.close();
                } catch (IOException e) {
                    // would be unusual to get here?
                    e.printStackTrace();
                }
            }
        }

        Map withEs = new HashMap();
        List invalidJars = new ArrayList();
        List withoutEs = new ArrayList();
        boolean failuresOccured = false;
        File[] children = inputdir.listFiles(new JARFileNameFilter());
        int totalsize = children.length;
        int nSourceBundles = 0;
        int nInExceptionList = 0;
        int checked = 0;
        int nProjectTags = 0;
        for (int i = 0; i < totalsize; i++) {
            File child = children[i];
            String name = child.getName();

            String bundleName = getBundleName(name);
            if (bundleName != null) {
                if (bundleName.endsWith(".source")) {
                    nSourceBundles++;
                } else if (esExceptions.contains(bundleName)) {
                    nInExceptionList++;
                } else {
                    checked++;
                    try {
                        String es = getESFromJAR(child);
                        if ((es != null) && es.contains("project=")) {
                            nProjectTags++;
                        }
                        if ((es != null) && (!es.isEmpty())) {
                            // has ES
                            incrementCounts(withEs, es);
                        } else {
                            // no ES
                            trackOmissions(withoutEs, child);
                        }
                    } catch (SecurityException e) {
                        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=378764
                        invalidJars.add(name);
                    }
                }
            }
        }
        printreport(invalidJars, withEs, withoutEs, totalsize, checked, nProjectTags, nSourceBundles, nInExceptionList);

        return failuresOccured;
    }

    private void printreport(List invalidJars, Map withEs, List withoutESs, int totalsize, int checked, int nProjectTags,
            int nSourceBundles, int nInExceptionList) throws FileNotFoundException {

        ReportWriter reportWriter = getReportWriter();
        try {
            reportWriter.writeln();
            reportWriter.writeln("   Directory checked: " + getBundleDirectory());
            reportWriter.writeln("   Number of source bundles (not checked): " + nSourceBundles);
            reportWriter.writeln("   Number in exception list (not checked): " + nInExceptionList);
            reportWriter.writeln("   Checked " + checked + " of " + totalsize + " jars.");
            reportWriter.writeln();
            reportWriter.writeln();
            reportWriter.writeln("   Number of bundles with an Eclipse-SourceReferences: " + totalCount(withEs));
            reportWriter.writeln("        Number of those with 'project=': " + nProjectTags + " attribute.");
            reportWriter.writeln("   Number of bundles without an Eclipse-SourceReferences: " + withoutESs.size());
            reportWriter.writeln();
            reportWriter.writeln();
            reportWriter.writeln("  Bundles with Eclipse-SourceReferences (total: " + totalCount(withEs) + ")");
            reportWriter.writeln();
            printInvalidJars(invalidJars, reportWriter);
            Collection allESs = withEs.keySet();
            List allESList = new ArrayList(allESs);
            Collections.sort(allESList);
            for (Object object : allESList) {
                // Integer count = (Integer) withEs.get(object);
                // reportWriter.writeln("   " + count.intValue() + "  " +
                // object);
                reportWriter.writeln("   " + object);
            }
            reportWriter.writeln();
            reportWriter.writeln();
            reportWriter.writeln("    Bundles without an Eclipse-SourceReferences (total: " + withoutESs.size() + ")");
            Collections.sort(withoutESs);
            for (Object object : withoutESs) {
                reportWriter.writeln("       " + object);
            }
            reportWriter.writeln();
        } finally {
            reportWriter.close();
        }
    }

    private int totalCount(Map bundlesWithEs) {

        Collection allCounts = bundlesWithEs.values();
        int total = 0;
        for (Iterator iterator = allCounts.iterator(); iterator.hasNext();) {
            Integer count = (Integer) iterator.next();
            total = total + count.intValue();
        }
        return total;
    }

    private void trackOmissions(List bundlesWithoutEs, File child) {
        bundlesWithoutEs.add(child.getName());

    }

    private void incrementCounts(Map esMap, String es) {
        Integer count = (Integer) esMap.get(es);
        if (count == null) {
            esMap.put(es, Integer.valueOf(1));
        } else {
            esMap.put(es, increment(count));
        }

    }

    private Integer increment(Integer count) {
        return Integer.valueOf(count.intValue() + 1);
    }

    private String getBundleName(String fullname) {
        String result = null;
        boolean parsable = nameParser.parse(fullname);
        if (parsable) {
            result = nameParser.getProjectString();
        }
        return result;
    }

    /*
     * Return the bundle id from the manifest pointed to by the given input
     * stream.
     */
    private String getESFromManifest(InputStream input, String path) {
        String es = null;
        try {
            Map attributes = ManifestElement.parseBundleManifest(input, null);
            es = (String) attributes.get(PROPERTY_ECLIPSE_SOURCEREFERENCES);
        } catch (BundleException | IOException e) {
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

        return es;
    }

    /*
     * The given file points to a bundle contained in an archive. Look into the
     * bundle manifest file to find the bundle identifier.
     */
    private String getESFromJAR(File file) {
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
            return getESFromManifest(input, file.getAbsolutePath());
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

}
