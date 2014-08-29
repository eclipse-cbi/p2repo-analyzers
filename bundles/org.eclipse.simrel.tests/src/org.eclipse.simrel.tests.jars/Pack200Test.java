/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use 
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading 
 * it as a properties file.
 ******************************************************************************/
package org.eclipse.simrel.tests.jars;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.simrel.tests.utils.FullJarNameParser;
import org.eclipse.simrel.tests.utils.JARFileNameFilter;
import org.eclipse.simrel.tests.utils.PackGzFileNameFilter;
import org.eclipse.simrel.tests.utils.ReportWriter;

/**
 * @since 3.3
 */
public class Pack200Test extends TestJars {

    private static final String      EXTENSION_JAR        = ".jar";
    private static final String      EXTENSION_PACEKD_JAR = ".jar.pack.gz";
    private static FullJarNameParser nameParser           = new FullJarNameParser();
    private static final String      outputFilename       = "pack200data.txt";

    public static void main(String[] args) {

        Pack200Test testlayout = new Pack200Test();
        testlayout.setDirectoryToCheck("D:\\temp\\staging\\");
        try {
            testlayout.testBundlePack();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean testBundlePack() throws IOException {

        File inputdir = new File(getBundleDirectory());
        boolean failuresOccured = checkFilesPacked(inputdir);
        return failuresOccured;
    }

    private boolean checkFilesPacked(File inputdir) throws IOException {
        // reset/initialize errors
        List errors = new ArrayList();
        List invalidJars = new ArrayList();
        boolean failuresOccured = false;
        File[] jarchildren = inputdir.listFiles(new JARFileNameFilter());
        File[] packedchildren = inputdir.listFiles(new PackGzFileNameFilter());
        File[] nopackedFile = nopackedFile(jarchildren, packedchildren);
        int totalsize = nopackedFile.length;
        int checked = 0;
        for (int i = 0; i < totalsize; i++) {
            File child = nopackedFile[i];
            String name = child.getName();

            // assume directory if not file
            if (child.isFile()) {
                checked++;
                try {
                    String basicName = getBasicName(name, EXTENSION_JAR);
                    boolean valid = nameParser.parse(basicName);
                    // String bundlename = basicName;
                    if (!valid) {
                        errors.add(name + " does not have a valid version (it is unparsable)");
                    } else {
                        if (containsJava(child)) {
                            // bundlename = nameParser.getProjectString();
                            errors.add(name + " contains java but is not packed");
                        }
                    }
                } catch (SecurityException e) {
                    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=378764
                    invalidJars.add(name);
                }
            }
        }
        printSummary(invalidJars, errors, jarchildren, packedchildren, totalsize, checked);
        if (errors.size() > 0) {
            failuresOccured = true;
        }
        return failuresOccured;
    }

    private void printSummary(List invalidJars, List errors, File[] jarchildren, File[] packedchildren, int totalsize, int checked)
            throws IOException {

        ReportWriter reportWriter = createReportWriter(outputFilename);
        try {
            reportWriter.writeln();
            reportWriter.writeln("   Directory checked: " + getBundleDirectory());
            reportWriter.writeln();
            reportWriter.writeln(" Check of packed and not packed bundles.");
            reportWriter.writeln("   Number of jar files " + jarchildren.length);
            reportWriter.writeln("   Number of pack.gz files " + packedchildren.length);
            reportWriter.writeln("   Difference, number of jar files to check: " + (jarchildren.length - packedchildren.length));
            reportWriter.writeln("   Checked " + checked + " of " + totalsize + ".");
            reportWriter.writeln("   Errors found: " + errors.size());

            printInvalidJars(invalidJars, reportWriter);
            
            if (errors.size() > 0) {
                Collections.sort(errors);
                for (Iterator iter = errors.iterator(); iter.hasNext();) {
                    reportWriter.writeln(iter.next());
                }
            }
        } finally {
            reportWriter.close();
        }
    }

    private File[] nopackedFile(File[] jarchildren, File[] packedchildren) {
        ArrayList results = new ArrayList();
        for (int i = 0; i < jarchildren.length; i++) {
            File file = jarchildren[i];
            if (!contains(packedchildren, file)) {
                results.add(file);
            }
        }
        File[] fileArray = new File[results.size()];
        int i = 0;
        for (Iterator iterator = results.iterator(); iterator.hasNext();) {
            fileArray[i++] = (File) iterator.next();
        }
        return fileArray;
    }

    private boolean contains(File[] packedchildren, File file) {
        boolean result = false;
        for (int i = 0; i < packedchildren.length; i++) {
            if (getBasicName(packedchildren[i].getName(), EXTENSION_PACEKD_JAR).equals(getBasicName(file.getName(), EXTENSION_JAR))) {
                result = true;
                break;
            }
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
                } else if (entry.getName().endsWith(".jar")) {
                    InputStream input = jar.getInputStream(entry);
                    if (containsJava(input)) {
                        containsJava = true;
                        break;
                    }
                }
            }
        } catch (ZipException e) {
            System.out.println("Failed to open jar file (zip exception): " + jarfile.getAbsolutePath());
        
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
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

    private String getBasicName(String fullname, String extension) {
        String result = fullname;
        int pos = fullname.lastIndexOf(extension);
        if (pos >= 0) {
            result = fullname.substring(0, pos);
        }
        return result;
    }

}
