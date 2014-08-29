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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.simrel.tests.utils.JARFileNameFilter;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @since 3.3
 */
public class TestLayoutTest extends TestJars {

    private static final String outputFilename       = "layoutCheck.txt";
    private static final String EXTENSION_JAR        = ".jar";
    private static final String EXTENSION_PACEKD_JAR = ".pack.gz";
    private static final String EXTENSION_ZIP        = ".zip";
    private static final String PROPERTY_BUNDLE_ID   = "Bundle-SymbolicName";
    private String              configFilename       = "config.properties";
    private static final String KEY_DFT_BIN_JAR      = "default.binary.jar";
    private static final String KEY_DFT_SRC_JAR      = "default.source.jar";
    private static final String KEY_DFT_FEATURE      = "default.feature";
    private Properties          config;
    private List                errors               = new ArrayList();

    public static void main(String[] args) {

        TestLayoutTest testlayout = new TestLayoutTest();
        testlayout.setDirectoryToCheck("D:\\temptest");
        testlayout.setTempWorkingDir("D:/temp");
        try {
            testlayout.testLayout();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addError(String message) {
        errors.add(message);
    }

    public boolean testLayout() throws IOException {
        boolean result = false;
        try {
            createReportWriter(outputFilename);
            getReportWriter().writeln("Check files and layout in bundles and features.");
            boolean featureFailures = testFeatureLayout();
            boolean bundleFailures = testBundleLayout();
            result = featureFailures || bundleFailures;
        } finally {
            getReportWriter().close();
        }
        return result;
    }

    private boolean testBundleLayout() throws IOException {

        errors = new ArrayList();
        boolean failuresOccured = false;

        File inputdir = new File(getBundleDirectory());

        File[] children = inputdir.listFiles(new JARFileNameFilter());
        int totalsize = children.length;
        int checked = 0;
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String id = getBundleId(child);
            if (id != null) {
                boolean sourceIU = isSourceIUName(id);
                processBundle(child, getExpected(id, sourceIU));
                checked++;
            }

        }
        getReportWriter().writeln();
        getReportWriter().writeln("   Checking: " + getBundleDirectory());
        getReportWriter().writeln("   Checked " + checked + " of " + totalsize + ".");
        getReportWriter().writeln("   Errors found: " + errors.size());

        if (errors.size() > 0) {
            Collections.sort(errors);
            for (Iterator iter = errors.iterator(); iter.hasNext();) {
                getReportWriter().writeln(iter.next());
            }
            failuresOccured = true;
        }
        return failuresOccured;
    }

    private boolean isSourceIUName(String id) {
        return id.endsWith(".source") || id.endsWith(".infopop") || id.endsWith(".doc.user") || id.endsWith(".doc")
                || id.endsWith(".doc.isv") || id.endsWith(".doc.dev") || id.endsWith(".doc.api") || id.endsWith("standard.schemas")
                || id.endsWith(".branding");
    }

    /*
     * Check the configuration file and return a set of regular expressions
     * which match the list of files that are expected to be in the bundle.
     */
    private Set getExpected(String id, boolean source) {
        return findConfiguration(id, source, false);
    }

    private Set getFeatureExpected(String id, boolean source, boolean zip) {
        return findConfiguration(id, source, true);
    }

    private Set<String> findConfiguration(String id, boolean source, boolean feature) {
        // is the config cached?
        if (config == null) {
            loadConfig();
        }
        String line = config.getProperty(id);
        if (line == null) {
            if (feature) {
                line = config.getProperty(KEY_DFT_FEATURE);
            } else {
                if (source) {
                    line = config.getProperty(KEY_DFT_SRC_JAR);
                } else {
                    line = config.getProperty(KEY_DFT_BIN_JAR);
                }
            }
        }
        if (line == null) {
            handleFatalError("Unable to load settings for: " + id);
        }
        if (id.endsWith(".source")) {
            // cut the source suffix to get the binary IU id
            line = MessageFormat.format(line, id.substring(0, id.lastIndexOf('.')));
        } else {
            line = MessageFormat.format(line, id);
        }
        Set<String> result = new HashSet<String>();
        for (StringTokenizer tokenizer = new StringTokenizer(line, ","); tokenizer.hasMoreTokens();) {
            result.add(tokenizer.nextToken().trim());
        }
        return result;
    }

    private void loadConfig() {
        config = new Properties();
        InputStream input = null;
        try {
            // if we can read this file, it's been set by caller
            File configFile = new File(getConfigFilename());
            if (configFile.exists()) {
                input = new FileInputStream(configFile);
            } else {
                // else, use the default we "ship"
                input = this.getClass().getResourceAsStream(getConfigFilename());
            }

            if (input == null) {
                handleFatalError("Unable to load configuration file.");
            }
            config.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /*
     * Process the bundle at the specified location, with the given set of
     * expected results.
     */
    private void processBundle(File file, Set expected) {
        if (file.isDirectory()) {
            String[] array = (String[]) expected.toArray(new String[expected.size()]);
            processDir("", file, array);
            for (int i = 0; i < array.length; i++) {
                if (array[i] != null) {
                    addError("Missing " + array[i] + " in dir: " + file.getAbsolutePath());
                }
            }
        } else {
            processArchive(file, (String[]) expected.toArray(new String[expected.size()]));
        }
    }

    private void processFeature(File file, Set expected) {
        if (file.isDirectory()) {
            String[] array = (String[]) expected.toArray(new String[expected.size()]);
            processDir("", file, array);
            for (int i = 0; i < array.length; i++) {
                if (array[i] != null) {
                    addError("Missing " + array[i] + " in dir: " + file.getAbsolutePath());
                }
            }
        } else {
            processArchive(file, (String[]) expected.toArray(new String[expected.size()]));
        }
    }

    /*
     * The bundle is an archive. Make sure it has the right contents.
     */
    private void processArchive(File file, String[] expected) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(file, ZipFile.OPEN_READ);
            for (Enumeration e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                String name = entry.getName();
                for (int i = 0; i < expected.length; i++) {
                    String pattern = expected[i];
                    if (pattern == null) {
                        continue;
                    }
                    try {
                        if (name.matches(pattern)) {
                            expected[i] = null;
                        }
                    } catch (PatternSyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
            for (int i = 0; i < expected.length; i++) {
                if (expected[i] != null) {
                    addError("Missing " + expected[i] + " in file: " + file.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /*
     * The bundle is in a directory.
     */
    private void processDir(String root, File dir, String[] expected) {
        File[] children = dir.listFiles();
        for (int index = 0; index < children.length; index++) {
            File child = children[index];
            String name = root.length() == 0 ? child.getName() : root + '/' + child.getName();
            if (child.isDirectory()) {
                processDir(name, child, expected);
                continue;
            }
            for (int i = 0; i < expected.length; i++) {
                String pattern = expected[i];
                if (pattern == null) {
                    continue;
                }
                try {
                    if (name.matches(pattern)) {
                        expected[i] = null;
                    }
                } catch (PatternSyntaxException ex) {
                    // ex.printStackTrace();
                    addError(ex.getMessage());
                    continue;
                }
            }
        }
    }

    /*
     * Return the bundle id from the manifest pointed to by the given input
     * stream.
     */
    private String getBundleIdFromManifest(InputStream input, String path) {
        String id = null;
        try {
            Map attributes = ManifestElement.parseBundleManifest(input, null);
            id = (String) attributes.get(PROPERTY_BUNDLE_ID);
            if ((id == null) || (id.length() == 0)) {
                addError("BundleSymbolicName header not set in manifest for bundle: " + path);
            } else {
                // identifier can be followed by attributes such as
                // 'singleton'
                int pos = id.indexOf(';');
                if (pos > 0) {
                    id = id.substring(0, pos);
                }
            }

        } catch (BundleException e) {
            // e.printStackTrace();
            addError(e.getMessage());
        } catch (IOException e) {
            // e.printStackTrace();
            addError(e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return id;
    }

    /*
     * Return the bundle identifier for the bundle contained in the given
     * archive/directory.
     */
    private String getBundleId(File file) {
        String id = null;
        if (file.isDirectory()) {
            id = getBundleIdFromDir(file);
        } else if (file.getName().toLowerCase().endsWith(EXTENSION_ZIP)) {
            id = getBundleIdFromZIP(file);
        } else if (file.getName().toLowerCase().endsWith(EXTENSION_JAR)) {
            id = getBundleIdFromJAR(file);
        }
        return id;
    }

    private String getBundleIdFromZIP(File file) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(file);
            for (Enumeration e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.getName().matches("^.*/" + JarFile.MANIFEST_NAME)) {
                    InputStream input = zip.getInputStream(entry);
                    try {
                        return getBundleIdFromManifest(input, file.getAbsolutePath());
                    } finally {
                        try {
                            input.close();
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                }
            }
        } catch (IOException ex) {
            // ex.printStackTrace();
            addError(ex.getMessage());
        } finally {
            try {
                if (zip != null) {
                    zip.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
        addError("Bundle manifest (MANIFEST.MF) not found in bundle: " + file.getAbsolutePath());
        return null;
    }

    /*
     * The given file points to an expanded bundle on disc. Look into the bundle
     * manifest file to find the bundle identifier.
     */
    private String getBundleIdFromDir(File dir) {
        String id = null;
        File manifestFile = new File(dir, JarFile.MANIFEST_NAME);
        if (!manifestFile.exists() || !manifestFile.isFile()) {
            addError("Bundle manifest (MANIFEST.MF) not found at: " + manifestFile.getAbsolutePath());
        } else {
            try {
                id = getBundleIdFromManifest(new FileInputStream(manifestFile), manifestFile.getAbsolutePath());
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
                addError(e.getMessage());
            }
        }
        return id;
    }

    /*
     * The given file points to a bundle contained in an archive. Look into the
     * bundle manifest file to find the bundle identifier.
     */
    private File getFileFromPACKEDJAR(File file) {

        File tmpjar = null;
        try {
            JarProcessor jarprocessor = JarProcessor.getUnpackProcessor(null);
            jarprocessor.setWorkingDirectory(getTempWorkingDir());
            tmpjar = jarprocessor.processJar(file);
        } catch (IOException e) {
            addError(e.getMessage());
        }
        return tmpjar;
    }

    public String getConfigFilename() {
        return configFilename;
    }

    public void setConfigFilename(String configFilename) {
        this.configFilename = configFilename;
    }

    /*
     * The given file points to a bundle contained in an archive. Look into the
     * bundle manifest file to find the bundle identifier.
     */
    private String getBundleIdFromJAR(File file) {
        InputStream input = null;
        JarFile jar = null;
        try {
            jar = new JarFile(file, false, ZipFile.OPEN_READ);
            JarEntry entry = jar.getJarEntry(JarFile.MANIFEST_NAME);
            if (entry == null) {
                addError("Bundle does not contain a MANIFEST.MF file: " + file.getAbsolutePath());
                return null;
            }
            input = jar.getInputStream(entry);
            return getBundleIdFromManifest(input, file.getAbsolutePath());
        } catch (IOException e) {
            // e.printStackTrace();
            addError(e.getMessage());
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

    private boolean testFeatureLayout() throws IOException {

        errors = new ArrayList();
        boolean failuresOccurred = false;
        File inputdir = new File(getFeatureDirectory());
        File[] children = inputdir.listFiles(new JARFileNameFilter());
        int totalsize = children.length;
        int checked = 0;
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.getName().toLowerCase().endsWith(EXTENSION_PACEKD_JAR)) {
                child = getFileFromPACKEDJAR(child);
            }
            if (child != null) {
                String id = getFeatureId(child);
                if (id != null) {
                    processFeature(child, getFeatureExpected(id, true, child.getName().endsWith(EXTENSION_ZIP)));
                    checked++;
                }
            }
        }
        getReportWriter().writeln();
        getReportWriter().writeln("   Checking: " + getFeatureDirectory());
        getReportWriter().writeln("   Checked " + checked + " of " + totalsize + ".");
        getReportWriter().writeln("   Errors found: " + errors.size());
        if (errors.size() > 0) {
            Collections.sort(errors);
            for (Iterator iter = errors.iterator(); iter.hasNext();) {
                getReportWriter().writeln(iter.next());
            }
            failuresOccurred = true;
        }
        return failuresOccurred;

    }

    private String getFeatureId(File file) {
        String id = null;
        if (file.isDirectory()) {
            id = getFeatureIdFromDir(file);
        } else if (file.getName().toLowerCase().endsWith(EXTENSION_JAR)) {
            id = getFeatureIdFromJAR(file);
        }
        return id;
    }

    private String getFeatureIdFromJAR(File file) {
        InputStream input = null;
        JarFile jar = null;
        String id = null;
        try {
            jar = new JarFile(file, false, ZipFile.OPEN_READ);
            JarEntry entry = jar.getJarEntry("feature.xml");
            if (entry == null) {
                addError("Feature jar does not contain a feature.xml file: " + file.getAbsolutePath());
            }
            input = jar.getInputStream(entry);
            id = getFeatureFromFeatureXML(input);
        } catch (IOException e) {
            // e.printStackTrace();
            addError(e.getMessage());
        } catch (ParserConfigurationException e) {
            addError(e.getMessage());
        } catch (SAXException e) {
            addError(e.getMessage());
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
        return id;
    }

    private String getFeatureIdFromDir(File dir) {
        String id = null;
        File featureFile = new File(dir, "feature.xml");
        if (!featureFile.exists() || !featureFile.isFile()) {
            addError("Feature.xml not found at: " + featureFile.getAbsolutePath());
        } else {
            id = getFeatureFromFeatureXML(featureFile);
        }
        return id;
    }

    private String getFeatureFromFeatureXML(File file) {
        Document document = getDOM(file);
        return getFeatureIdFromDOM(document);
    }

    private String getFeatureIdFromDOM(Document document) {
        String id = null;
        if (document != null) {
            NodeList featureElements = document.getElementsByTagName("feature");
            Element featureElement = null;
            if (featureElements.getLength() > 0) {
                Node featureNode = featureElements.item(0);
                if (featureNode instanceof Element) {
                    featureElement = (Element) featureNode;
                }
            }
            if (featureElement != null) {
                NamedNodeMap aNamedNodeMap = featureElement.getAttributes();
                Node idAttribute = aNamedNodeMap.getNamedItem("id");
                id = idAttribute.getNodeValue();
            }
        }
        return id;
    }

    private String getFeatureFromFeatureXML(InputStream stream) throws ParserConfigurationException, SAXException, IOException {
        Document document = getDOM(stream);
        return getFeatureIdFromDOM(document);
    }

    private Document getDOM(File file) {

        Document aDocument = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            InputSource inputSource = new InputSource(reader);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            aDocument = builder.parse(inputSource);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore this one
                }
            }
        }

        if (aDocument == null) {
            handleFatalError("Error: could not parse xml in classpath file: " + file.getAbsolutePath());
        }
        return aDocument;

    }

    private Document getDOM(InputStream stream) throws ParserConfigurationException, SAXException, IOException {

        Document aDocument = null;
        InputSource inputSource = new InputSource(stream);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        aDocument = builder.parse(inputSource);

        return aDocument;

    }

}
