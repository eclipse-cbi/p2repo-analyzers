package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.cbi.p2repo.analyzers.RepoTestsConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CheckGreedy extends TestRepo {

    public CheckGreedy(RepoTestsConfiguration configurations) {
        super(configurations);
    }

    private InputStream inputStream;

    public boolean testGreedyOptionals() throws ParserConfigurationException, SAXException, IOException {
        Document document = getDocument();
        return checkOptionals(document);
    }

    private Document getDocument() throws ParserConfigurationException, SAXException, IOException {
        Document document = null;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Reader characterStream = getCharacterStreamFromURL();
        if (characterStream != null) {
            InputSource is = new InputSource(characterStream);
            document = documentBuilder.parse(is);
        }
        return document;
    }

    /*
     * TODO: will need more work and testing when using with http:// scheme
     */
    private Reader getCharacterStreamFromURL() throws IOException {

        Reader characterStream = null;
        URL url = null;
        InputStream rawinputStream = null;
        inputStream = null;
        URLConnection connection = null;
        System.out.println("repoURLToTest: " + getRepoURLToTest());
        url = new URL(getRepoURLToTest() + "/" + "content.jar");
        try {
            connection = url.openConnection();
            rawinputStream = connection.getInputStream();
            ZipInputStream zipStream = new ZipInputStream(rawinputStream);
            ZipEntry zipEntry = zipStream.getNextEntry();
            // should only ever be one entry, but, sometimes not, sometimes
            // META-INF, etc., ends
            // up in these

            String entryName = null;
            do {
                entryName = zipEntry.getName();
            } while ((entryName != null) && (!entryName.equals("content.xml")) && null != (zipEntry = zipStream.getNextEntry()));

            if ((entryName != null) && !entryName.equals("content.xml")) {
                throw new IllegalArgumentException("zip entry 'content.xml' was not found as expected.");
            }

            System.out.println("Found: " + getRepoURLToTest() + "/" + "content.jar");
            String localFilePathName = createLocalFile(zipStream, zipEntry);
            File file = new File(localFilePathName);
            System.out.println("Created. converted, and using local file: " + file.getAbsolutePath());
            inputStream = new FileInputStream(file);

        } catch (FileNotFoundException e) {
            // try xml version

            url = new URL(getRepoURLToTest() + "/" + "content.xml");

            try {
                connection = url.openConnection();
                inputStream = connection.getInputStream();
            } catch (FileNotFoundException e1) {
                throw new IllegalArgumentException("Neither content.jar nor content.xml file found at URL: " + getRepoURLToTest());
            }

        }

        if (inputStream != null) {
            characterStream = new InputStreamReader(inputStream);
        }

        return characterStream;
    }

    private String createLocalFile(ZipInputStream zipStream, ZipEntry zipEntry) throws FileNotFoundException, IOException {
        // Once we get the entry from the stream, the stream is
        // positioned read to read the raw data, and we keep
        // reading until read returns 0 or less.
        byte[] buffer = new byte[8192];
        String outpath = getConfigurations().getTempWorkingDir() + "/" + zipEntry.getName();
        try (FileOutputStream output = new FileOutputStream(outpath)) {
            int len = 0;
            while ((len = zipStream.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        }
        return outpath;
    }

    protected boolean xcheckGreedyOptionals(Document document) throws IOException {
        NodeList iuElements = document.getElementsByTagName("unit");
        int nUnits = iuElements.getLength();
        // System.out.println("Number of IUs: " + nUnits);
        for (int i = 0; i < nUnits; i++) {
            Node iuNode = iuElements.item(i);
            String idString = getAttributeValue(iuNode, "id");
            System.out.println("id: " + idString);

        }
        // System.out.println("= = = = = ");
        checkOptionals(document);
        return false;
    }

    private String getAttributeValue(Node node, String attribute) {
        String result = null;
        if (node != null) {
            NamedNodeMap attributes = node.getAttributes();
            Node id = attributes.getNamedItem(attribute);
            if (id != null) {
                result = id.getNodeValue();
            }
        }
        return result;
    }

    private boolean checkOptionals(Document document) throws IOException {
        boolean result = false;
        if (document == null) {
            result = false;
        } else {

            List<String> intenionallyTrueOptionals = new ArrayList<>();
            List<String> intenionallyImpliedTrueOptionals = new ArrayList<>();
            List<String> intenionallyFalseOptionals = new ArrayList<>();
            // List will be list of parent IU Nodes that contain the optional
            Map<String, List> blameIU = new HashMap<>();
            NodeList iuElements = document.getElementsByTagName("required");
            int nUnits = iuElements.getLength();
            // System.out.println("Number of total required elements found: "
            // +
            // nUnits);
            for (int i = 0; i < nUnits; i++) {
                Node iuNode = iuElements.item(i);
                NamedNodeMap attributes = iuNode.getAttributes();
                String optional = getAttributeValue(attributes, "optional");
                if (optional != null) {
                    String namespace = getAttributeValue(attributes, "namespace");
                    String name = getAttributeValue(attributes, "name");
                    // String range = getAttributeValue(attributes,
                    // "range");
                    if (!name.endsWith(".feature.group") && !namespace.equals("org.eclipse.equinox.p2.eclipse.type")) {
                        StringBuffer stringBuffer = new StringBuffer();
                        stringBuffer.append("\t" + "name: " + name);
                        stringBuffer.append("\t\t\t\t" + "namespace: ");
                        String shortname = null;
                        if (namespace.equals("osgi.bundle")) {
                            shortname = "bundle";
                        } else if (namespace.equals("java.package")) {
                            shortname = "package";
                        } else {
                            System.out.println("namespace: " + namespace);
                            shortname = namespace;
                        }
                        stringBuffer.append(shortname);
                        // leave out for now to avoid clutter
                        // stringBuffer.append("\t" + "range: " + range);
                        // doesn't work
                        // Node unitNode = idoptional.getParentNode();
                        // System.out.println("parent unit: " +
                        // getAttributeValue(unitNode, "id"));
                        // leave out for now to avoid clutter
                        // stringBuffer.append("\t" + "optional: " +
                        // optional);
                        String greedy = getAttributeValue(attributes, "greedy");
                        if (greedy == null) {
                            stringBuffer.append("\t" + "greedy: true (implied)");
                            intenionallyImpliedTrueOptionals.add(name);

                            addBlameIU(blameIU, iuNode, name);
                        } else {
                            stringBuffer.append("\t" + "greedy: " + greedy);
                            if (greedy.equals("true")) {
                                intenionallyTrueOptionals.add(name);
                            } else {
                                intenionallyFalseOptionals.add(name);
                            }
                        }
                        // System.out.println(stringBuffer);
                    }
                }
            }
            printHTMLReport(nUnits, intenionallyTrueOptionals, intenionallyImpliedTrueOptionals, intenionallyFalseOptionals,
                    blameIU);
            result = true;
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return result;
    }

    private void addBlameIU(Map<String, List> blameIU, Node iuNode, String name) {

        // Walk "up" the tree to get parent element named "unit".
        Node iuelement = walkbackToUnit(iuNode);
        String unitID = getID(iuelement);
        // first see if there is one already, if not create array, else just add
        // to array
        List anodeList = blameIU.get(name);
        if (anodeList == null) {
            anodeList = new ArrayList<String>();
            anodeList.add(unitID);
            blameIU.put(name, anodeList);
        } else {
            anodeList.add(unitID);
        }
    }

    private String getID(Node iuelement) {
        NamedNodeMap attributes = iuelement.getAttributes();
        Node idattr = attributes.getNamedItem("id");
        return idattr.getNodeValue();
    }

    private Node walkbackToUnit(Node iuNode) {
        if (iuNode.getNodeName().equals("unit")) {
            return iuNode;
        }
        Node pnode = iuNode.getParentNode();
        if (pnode == null) {
            return null;
        }

        return walkbackToUnit(pnode);
    }

    private void printHTMLReport(int nUnits, List<String> intenionallyTrueOptionals, List<String> intenionallyImpliedTrueOptionals,
            List<String> intenionallyFalseOptionals, Map<String, List> blameIUs) throws IOException {

        FileWriter outfile = createOutputFile();
        try (outfile) {
            List<String> conflictingStatements = new ArrayList<>();
            conflictingStatements = overlapping(intenionallyFalseOptionals, intenionallyImpliedTrueOptionals);
            conflictingStatements.addAll(overlapping(intenionallyFalseOptionals, intenionallyTrueOptionals));

            // we don't count "overlapping lists" here, since that'd double
            // count some
            int listsTotal = intenionallyFalseOptionals.size() + intenionallyImpliedTrueOptionals.size()
                    + intenionallyTrueOptionals.size();

            printHeader(outfile, 1, "Report on optional runtime requirements and greediness");
            printparagraph(outfile, "Using repository content metadata from repo at " + getRepoURLToTest());
            printparagraph(outfile, "Total number of 'requried' elements found: " + nUnits);
            printparagraph(outfile, "Total number of 'requried' elements found with 'optional=\"true\"' attribute: " + listsTotal);
            printHeader(
                    outfile,
                    2,
                    "Probable problem. Conflicting specifications. (Intersection of other lists.) Optional runtime requirement sometimes with greedy install, sometimes not.");
            printLinesProvider(outfile, conflictingStatements, blameIUs);
            printHeader(outfile, 2, "Probable problem. Optional requirements with implicit greedy install (old publisher default)");
            printLinesProvider(outfile, intenionallyImpliedTrueOptionals, blameIUs);
            printHeader(outfile, 2,
                    "Unusual cases, but assumed intended. Optional runtime requirement with explicit greedy install.");
            printLinesProvider(outfile, intenionallyTrueOptionals);
            printHeader(outfile, 2,
                    "Correct cases. Optional runtime requirements with explicit no-greedy install (new publisher default)");
            printLinesProvider(outfile, intenionallyFalseOptionals);
        }
    }

    private List<String> overlapping(List<String> intenionallyFalseOptionals, List<String> intenionallyImpliedTrueOptionals) {
        Set<String> intersectionSet = new HashSet<>();
        // if requirement is specified in both lists, it sometimes is
        // optional=true greedy=true, but sometimes
        // optional=true greedy=false.

        for (String nongreedyRequirement : intenionallyFalseOptionals) {
            if (intenionallyImpliedTrueOptionals.contains(nongreedyRequirement)) {
                intersectionSet.add(nongreedyRequirement);
            }
        }
        return new ArrayList<>(intersectionSet);
    }

    private FileWriter createOutputFile() throws IOException {
        File outfile = null;
        String testDirName = getReportOutputDirectory();
        outfile = new File(testDirName, "greedyReport.html");
        System.out.println("output: " + outfile.getAbsolutePath());
        return new FileWriter(outfile);
    }

    private String getAttributeValue(NamedNodeMap attributes, String name) {
        String result = null;
        Node attrnode = attributes.getNamedItem(name);
        if (attrnode != null) {
            result = attrnode.getNodeValue();
        }
        return result;
    }

    private void printLinesProvider(FileWriter out, List<String> names) throws IOException {

        Set<String> namesSet = new HashSet<>(names);
        out.write("<p>Total Count: " + names.size() + EOL);
        out.write("<p>Unique Count: " + namesSet.size() + EOL);
        out.write("<ol>" + EOL);

        List<String> sortedUniqueNames = new ArrayList<>(namesSet);

        Collections.sort(sortedUniqueNames);

        for (String name : sortedUniqueNames) {
            printLineListItem(out, name);
        }
        out.write("</ol>" + EOL);
    }

    private void printLinesProvider(FileWriter out, List<String> names, Map<String, List> blameIUs) throws IOException {

        Set<String> namesSet = new HashSet<>(names);
        out.write("<p>Total Count: " + names.size() + EOL);
        out.write("<p>Unique Count: " + namesSet.size() + EOL);
        out.write("<ol>" + EOL);

        List<String> sortedUniqueNames = new ArrayList<>(namesSet);

        Collections.sort(sortedUniqueNames);

        for (String name : sortedUniqueNames) {
            printLineListItem(out, name);
            printInnerList(out, name, blameIUs);
        }
        out.write("</ol>" + EOL);
    }

    private void printInnerList(FileWriter out, String name, Map<String, List> blameIUs) throws IOException {
        List toblame = blameIUs.get(name);
        if ((toblame == null)) {
            return;
        }
        Collections.sort(toblame);
        out.write("<p>Number of IUs using optional, but greedy for this case: " + toblame.size() + EOL);
        out.write("<ol>" + EOL);
        for (Object iu : toblame) {
            printLineListItem(out, (String) iu);
        }
        out.write("</ol></p>" + EOL);
    }

}
