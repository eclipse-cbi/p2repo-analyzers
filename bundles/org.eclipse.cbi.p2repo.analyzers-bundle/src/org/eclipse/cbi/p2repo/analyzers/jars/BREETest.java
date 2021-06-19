/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 ******************************************************************************/
package org.eclipse.cbi.p2repo.analyzers.jars;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.eclipse.cbi.p2repo.analyzers.utils.BundleJarUtils;
import org.eclipse.cbi.p2repo.analyzers.utils.FullJarNameParser;
import org.eclipse.cbi.p2repo.analyzers.utils.JARFileNameFilter;
import org.eclipse.cbi.p2repo.analyzers.utils.ReportWriter;
import org.osgi.framework.Constants;

/**
 * @since 3.3
 */
public class BREETest extends TestJars {

    public BREETest(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    static class BREEFileData implements Comparable {
        String filename;
        String breeValue;

        public BREEFileData(String filename, String breeValue) {
            this.filename = filename;
            this.breeValue = breeValue;
        }

        public int compareTo(Object o) {
            // first sort on bree name, then on file/bundle name
            int result = breeValue.compareTo(((BREEFileData) o).breeValue);
            if (result == 0) {
                result = filename.compareTo(((BREEFileData) o).filename);
            }
            return result;
        }

    }

    private static final String            outputFilename = "breedata.txt";
    private static final FullJarNameParser nameParser     = new FullJarNameParser();
    private static final Pattern breeFilter = Pattern.compile("\\(&\\(osgi.ee=\\w+\\)\\(version=\\d.\\d\\)");
    private static final Pattern breeName = Pattern.compile("osgi.ee=\\w+");
    private static final Pattern breeVersion = Pattern.compile("version=\\d.\\d");

    public static void main(String[] args) {

        BREETest testlayout = new BREETest(RepoTestsConfiguration.createFromSystemProperties());
        testlayout.setDirectoryToCheck("/home/files/testSDKRepo");
        try {
            testlayout.testBREESettingRule();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean testBREESettingRule() throws IOException {

        createReportWriter(outputFilename);
        File inputdir = new File(getBundleDirectory());

        boolean failuresOccured;

        failuresOccured = checkBundleBREE(inputdir);

        return failuresOccured;
    }

    private boolean checkBundleBREE(File inputdir) throws IOException {
        // reset/initialize errors
        InputStream propertyStream = this.getClass().getResourceAsStream("exceptions.properties");
        Properties breeExceptionProperties = new Properties();
        String breeExceptions = "";
        try {
            breeExceptionProperties.load(propertyStream);
            breeExceptions = breeExceptionProperties.getProperty("breeExceptions");
            if (breeExceptions == null) {
                breeExceptions = "";
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
        Map javaWithBree = new HashMap();
        List invalidJars = new ArrayList();
        List nonjavaWithBree = new ArrayList();
        Map plugins = new HashMap();
        List javaWithoutBree = new ArrayList();
        int nonJavaNoBREE = 0;
        boolean failuresOccured = false;
        File[] children = inputdir.listFiles(new JARFileNameFilter());
        int totalsize = children.length;
        int checked = 0;
        for (int i = 0; i < totalsize; i++) {
            File child = children[i];
            String name = child.getName();
            String bundleName = getBundleName(name);
            if ((bundleName != null) && !breeExceptions.contains(bundleName)) {
                checked++;
                try {
                    String bree = BundleJarUtils.getJarManifestEntry(child, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
                    if (bree == null) {
                        bree = parseBREECapability(BundleJarUtils.getJarManifestEntry(child, Constants.REQUIRE_CAPABILITY));
                    }
                    boolean needsBree = needsBree(child);
                    if ((bree != null) && (!bree.isEmpty())) {
                        // has BREE, confirm is java file
                        if (needsBree) {
                            plugins.put(bundleName, bree);
                            incrementCounts(javaWithBree, bree);
                        } else {
                            trackFalseInclusions(nonjavaWithBree, child, bree);
                            failuresOccured = true;
                        }
                    } else {
                        // no BREE, confirm is non-java
                        if (needsBree) {
                            trackOmissions(javaWithoutBree, child);
                            failuresOccured = true;
                        } else {
                            nonJavaNoBREE++;
                        }
                    }
                } catch (SecurityException e) {
                    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=378764
                    invalidJars.add(name);
                }
            }

        }
        printreport(invalidJars, javaWithBree, nonjavaWithBree, javaWithoutBree, plugins, nonJavaNoBREE, totalsize, checked);

        return failuresOccured;
    }

    private String parseBREECapability(String reqCap) {
        if (reqCap != null) {
            String[] requires = reqCap.split(",");
            for (String require : requires) {
                String[] split = require.split(";");
                if ("osgi.ee".equals(split[0]) && split[1] != null && split[1].startsWith("filter:=")) {
                    String filter = split[1].substring(9, split[1].length() - 1);
                    Matcher m = breeFilter.matcher(filter);
                    List<String> brees = new ArrayList<>();
                    while (m.find()) {
                        String group = m.group();
                        Matcher nameMatcher = breeName.matcher(group);
                        Matcher verMatcher = breeVersion.matcher(group);
                        if (nameMatcher.find() && verMatcher.find()) {
                            String name = nameMatcher.group().split("=")[1];
                            String version = verMatcher.group().split("=")[1];
                            brees.add(name + "-" + version);
                        }
                    }
                    return brees.stream().collect(Collectors.joining(","));
                }
            }
        }
        return null;
    }

    private boolean needsBree(File child) {
        return exportsPackages(child) || containsJava(child);
    }

    private boolean exportsPackages(File child) {
        String entry = BundleJarUtils.getJarManifestEntry(child, Constants.EXPORT_PACKAGE);
        if (entry != null && !entry.isEmpty()) {
            return true;
        }
        return false;
    }

    private void printreport(List invalidJars, Map javaWithBree, List nonjavaWithBree, List javaWithoutBree, Map<String, String> plugins, int nonJavaNoBREE,
            int totalsize, int checked) throws IOException {

        ReportWriter reportWriter = getReportWriter();
        try {
            reportWriter.writeln();
            reportWriter.writeln("   Directory checked: " + getDirectoryToCheck());
            reportWriter.writeln();
            reportWriter.writeln("   Checked " + checked + " of " + totalsize + " jars.");
            reportWriter.writeln();

            printInvalidJars(invalidJars, reportWriter);

            reportWriter.writeln();
            reportWriter.writeln("  Bundles with appropriate use of Bundle-RequiredExecutionEnvironment (BREE):");
            reportWriter.writeln("   Java with BREE: " + totalCount(javaWithBree));
            reportWriter.writeln("   Non Java without BREE:" + nonJavaNoBREE);
            reportWriter.writeln();
            reportWriter.writeln("   Distribution of BREEs in Java Bundles ");
            reportWriter.writeln();
            Set allBREEs = javaWithBree.keySet();
            List allBREEList = new ArrayList(allBREEs);
            Collections.sort(allBREEList);
            for (Object object : allBREEList) {
                Integer count = (Integer) javaWithBree.get(object);
                reportWriter.printf("\t\t%5d\t%s\n", count.intValue(), object);
            }
            reportWriter.writeln();
            reportWriter.writeln("  Bundles with questionable absence or presence of BREE");
            reportWriter.writeln();
            reportWriter.writeln("    Java Bundles without a BREE: " + javaWithoutBree.size());
            reportWriter.writeln();
            Collections.sort(javaWithoutBree);
            for (Iterator iterator = javaWithoutBree.iterator(); iterator.hasNext();) {
                Object object = iterator.next();
                reportWriter.writeln("       " + object);
            }
            reportWriter.writeln();
            reportWriter.writeln("    Non Java Bundles with a BREE: " + nonjavaWithBree.size());
            reportWriter.writeln();
            Collections.sort(nonjavaWithBree);
            BREEFileData breefiledata = null;

            for (Iterator iterator = nonjavaWithBree.iterator(); iterator.hasNext();) {
                Object object = iterator.next();
                if (object instanceof BREEFileData) {
                    breefiledata = (BREEFileData) object;
                    reportWriter.printf("%24s\t%s\n", breefiledata.breeValue, breefiledata.filename);
                } else {
                    throw new Error("Programming error.");
                }
            }

            reportWriter.writeln();
            reportWriter.writeln("    List of all plug-ins with BREE: " + javaWithBree.size());
            reportWriter.writeln();

            plugins.entrySet().stream()
            .sorted(Map.Entry.<String, String>comparingByValue().reversed())
            .forEach(entry-> {
                try {
                    reportWriter.printf("%24s\t%s\n",entry.getValue(), entry.getKey());
                } catch (FileNotFoundException e) {
                    throw new Error("Programming error in List of all plug-ins with BREE");
                }
            }); // or any other terminal method

        } finally {
            reportWriter.close();
        }
    }

    private int totalCount(Map javaWithBree) {

        Collection allCounts = javaWithBree.values();
        int total = 0;
        for (Iterator iterator = allCounts.iterator(); iterator.hasNext();) {
            Integer count = (Integer) iterator.next();
            total = total + count.intValue();
        }
        return total;
    }

    private void trackOmissions(List javaWithoutBree, File child) {
        javaWithoutBree.add(child.getName());

    }

    private void trackFalseInclusions(List list, File child, String bree) {
        list.add(new BREEFileData(child.getName(), bree));

    }

    private void incrementCounts(Map breeMap, String bree) {
        Integer count = (Integer) breeMap.get(bree);
        if (count == null) {
            breeMap.put(bree, new Integer(1));
        } else {
            breeMap.put(bree, increment(count));
        }

    }

    private Integer increment(Integer count) {
        return new Integer(count.intValue() + 1);
    }

    private String getBundleName(String fullname) {
        String result = null;
        boolean parsable = nameParser.parse(fullname);
        if (parsable) {
            result = nameParser.getProjectString();
        }
        return result;
    }

    private boolean containsJava(File jarfile) {
        // We assume the file is a 'jar' file.
        boolean containsJava = false;
        JarFile jar = null;
        try {
            jar = new JarFile(jarfile, false, ZipFile.OPEN_READ);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    containsJava = true;
                    break;
                } else if (entry.getName().endsWith(".jar") && isInBundleClasspath(jarfile, entry.getName())) {
                    // jar must be a part of bundle classpath
                    InputStream input = jar.getInputStream(entry);
                    if (containsJava(input)) {
                        containsJava = true;
                        break;
                    }
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return containsJava;
    }

    private boolean isInBundleClasspath(File jarfile, String name) {
        String entry = BundleJarUtils.getJarManifestEntry(jarfile, Constants.BUNDLE_CLASSPATH);
        // not very accurate but enough. Normally we should split entry with','
        // and compare with equals()
        if (entry != null && entry.contains(name)) {
            return true;
        }
        return false;
    }

    private boolean containsJava(InputStream input) {
        // We assume the file is a 'jar' file.
        boolean containsJava = false;
        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(input);
            while (jarInputStream.available() > 0) {
                ZipEntry entry = jarInputStream.getNextEntry();
                if (entry != null) {
                    if (entry.getName().endsWith(".class")) {
                        containsJava = true;
                        break;
                    }
                }
            }

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
            if (jarInputStream != null) {
                try {
                    jarInputStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }

        }
        return containsJava;
    }

}
