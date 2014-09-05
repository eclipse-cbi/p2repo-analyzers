/*******************************************************************************
 * Copyright (c) 2010,2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.simrel.tests.repos;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

import org.eclipse.simrel.tests.BuildRepoTests;
import org.eclipse.simrel.tests.TestActivator;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

/**
 * Tests that licenses in the repository are consistent with the platform
 * feature license.
 * 
 * This was based on test code originally attached to bug 306627
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=306627
 */
public class TestRepo extends BuildRepoTests {
    protected static final boolean         DEBUG = false;
    protected static final String          EOL   = System.getProperty("line.separator", "\n");
    protected static final String          NBSP  = " &nbsp; ";
    protected static final String          BR    = "<br/>" + EOL;
    private IQueryResult<IInstallableUnit> allCurrentIUs;
    private IQueryResult<IInstallableUnit> allReferenceIUs;
    private String                         repoURLToTest;
    private String                         repoURLForReference;

    protected void println(FileWriter out, String wholeLine) throws IOException {
        out.write("<li>" + wholeLine + "</li>" + EOL);

    }

    protected void printRowln(FileWriter out, String wholeLine) throws IOException {
        out.write("<tr>" + wholeLine + "</tr>" + EOL);
    }

    protected void printStartTable(FileWriter out, String attributes) throws IOException {
        String attributeString = attributes;
        if (attributeString == null) {
            attributeString = "";
        }
        out.write("<table " + attributeString + ">" + EOL);
    }

    protected void printEndTable(FileWriter out) throws IOException {
        out.write("</table>" + EOL);
    }

    protected void printparagraph(FileWriter out, String wholeLine) throws IOException {
        out.write("<p>" + convertEOLtoBR(wholeLine) + "</p>" + EOL);

    }

    protected void printHeader(FileWriter out, int level, String wholeLine) throws IOException {
        out.write("<h" + level + ">" + wholeLine + "</h" + level + ">" + EOL);

    }

    private String convertEOLtoBR(String wholeLine) {
        String result = wholeLine;
        result = result.replaceAll("\\r\\n", "<br />\n");
        result = result.replaceAll("\\n", "<br />\n");
        result = result.replaceAll("\\r", "<br />\n");
        return result;
    }

    protected boolean isSpecial(IInstallableUnit iu) {

        // TODO: I assume 'executable roots', etc. have no readable name?
        /*
         * TODO: what are these special things? What ever they are, they have no
         * provider name. config.a.jre is identified as a fragment
         * (org.eclipse.equinox.p2.type.fragment). a.jre has no properties.
         */
        String iuId = iu.getId();
        boolean isSpecial = iuId.startsWith("a.jre") || iuId.startsWith("config.a.jre") || iuId.endsWith("_root")
                || iuId.contains(".executable.") || iuId.contains("configuration_root") || iuId.contains("executable_root")
                || iuId.startsWith("toolingorg.eclipse") || iuId.startsWith("tooling.");
        return isSpecial;
    }

    protected boolean isFeatureGroup(IInstallableUnit iu) {

        String iuId = iu.getId();
        boolean isFeatureGroup = iuId.endsWith("feature.group");
        return isFeatureGroup;
    }

    protected boolean isGroup(IInstallableUnit iu) {

        boolean isGroup = "true".equals(iu.getProperty("org.eclipse.equinox.p2.type.group"));
        return isGroup;
    }

    protected void printLineListItem(FileWriter outfileWriter, IInstallableUnit iu, String iuproperty) throws IOException {
        String iupropertyValue = iu.getProperty(iuproperty, null);
        String iuId = iu.getId();
        String iuVersion = iu.getVersion().toString();
        println(outfileWriter, iuId + NBSP + iuVersion + NBSP + BR + iupropertyValue);
    }

    protected void printLineListItem(FileWriter outfileWriter, IInstallableUnit iu, IInstallableUnit iuRef) throws IOException {
        // String iupropertyValue = iu.getProperty(iuproperty, null);
        String iuId = iu.getId();
        String iuVersion = iu.getVersion().toString();
        String iuRefVersion = iuRef.getVersion().toString();
        int diff = iuVersion.compareTo(iuRefVersion);
        println(outfileWriter, diff + NBSP + iuId + NBSP + iuRefVersion + NBSP + iuVersion);
    }

    protected void printLineRowItem(FileWriter outfileWriter, IInstallableUnit iu, IInstallableUnit iuRef) throws IOException {
        String iuId = iu.getId();
        String iuVersion = iu.getVersion().toString();
        String iuRefVersion = iuRef.getVersion().toString();
        printRowln(outfileWriter, "<td>" + iuId + "</td><td>" + iuRefVersion + "</td><td>" + iuVersion + "</td>");
    }

    protected void printLineListItem(FileWriter outfileWriter, String string) throws IOException {
        println(outfileWriter, string);
    }

    /**
     * Use for debugging and exploration
     * 
     * @param outFileWriter
     * @param iu
     * @throws IOException
     */
    protected void printAllProperties(FileWriter outFileWriter, IInstallableUnit iu) throws IOException {
        Map<String, String> properties = iu.getProperties();
        Set keys = properties.keySet();
        for (Object key : keys) {
            String value = properties.get(key);
            println(outFileWriter, key + " : " + value);
        }

    }

    protected IQueryResult<IInstallableUnit> getAllReferenceIUs() throws URISyntaxException, ProvisionException {
        if (allReferenceIUs == null) {
            String repoRefURL = getRepoURLForReference();
            if (repoRefURL.length() > 0) {
                allReferenceIUs = getAllIUscore(repoRefURL);
            }
        }
        return allReferenceIUs;
    }

    protected IQueryResult<IInstallableUnit> getAllIUs() throws URISyntaxException, ProvisionException {
        if (allCurrentIUs == null) {
            String repoURL = getRepoURLToTest();
            allCurrentIUs = getAllIUscore(repoURL);
        }
        return allCurrentIUs;
    }

    private IQueryResult<IInstallableUnit> getAllIUscore(String repoURL) throws URISyntaxException, ProvisionException {
        IQueryResult<IInstallableUnit> allIUs = null;
        URI repoLocation = new URI(repoURL);
        IMetadataRepositoryManager repomgr = getMetadataRepositoryManager();
        if (repomgr != null) {
            IMetadataRepository repo = repomgr.loadRepository(repoLocation, null);
            if (repo == null) {
                handleFatalError("no repository found at " + repoLocation.toString());
            } else {
                allIUs = repo.query(QueryUtil.createIUAnyQuery(), null);
                if (allIUs.isEmpty()) {
                    handleFatalError("no IUs in repository" + repoLocation.toString());
                }
            }
        } else {
            System.out.println("Could not getMetadataRepositoryManager");
        }
        return allIUs;
    }

    protected IQueryResult<IInstallableUnit> getAllGroupIUs() throws URISyntaxException, ProvisionException {
        String repoURL = getRepoURLToTest();
        return getAllGroupIUscore(repoURL);
    }

    protected IQueryResult<IInstallableUnit> getAllReferenceGroupIUs() throws URISyntaxException, ProvisionException {
        IQueryResult<IInstallableUnit> result = null;
        String repoURL = getRepoURLForReference();
        if (repoURL.length() > 0) {
            result = getAllGroupIUscore(repoURL);
        }
        return result;

    }

    private IQueryResult<IInstallableUnit> getAllGroupIUscore(String repoURL) throws URISyntaxException, ProvisionException {
        IQueryResult<IInstallableUnit> allIUs = null;
        URI repoLocation = new URI(repoURL);
        IMetadataRepository repo = getMetadataRepositoryManager().loadRepository(repoLocation, null);
        if (repo == null) {
            handleFatalError("no repository found at " + repoLocation.toString());
        } else {
            allIUs = repo.query(QueryUtil.createIUGroupQuery(), null);
            if (allIUs.isEmpty()) {
                handleFatalError("no IUs in repository " + repoLocation.toString());
            }
        }
        return allIUs;
    }

    protected static IMetadataRepositoryManager getMetadataRepositoryManager() {
        return (IMetadataRepositoryManager) getAgent().getService(IMetadataRepositoryManager.SERVICE_NAME);
    }

    protected static IProvisioningAgent getAgent() {
        // get the global agent for the currently running system
        return (IProvisioningAgent) ServiceHelper.getService(TestActivator.getContext(), IProvisioningAgent.SERVICE_NAME);
    }

    public String getRepoURLToTest() {
        if (repoURLToTest == null) {
            repoURLToTest = System.getProperty("repoURLToTest");

            if (repoURLToTest == null) {
                handleFatalError("the 'repoURLToTest' property was not set");
            }

        }
        // System.out.println("repoURLToTest: " + repoURLToTest);
        return repoURLToTest;
    }

    public String getRepoURLForReference() {
        if (repoURLForReference == null) {
            repoURLForReference = System.getProperty("repoURLForReference");

            if (repoURLForReference == null) {
                handleWarning("the 'repoURLForReference' property was not set");
                repoURLForReference = "";
            }
            if (repoURLForReference.length() > 0) {
                System.out.println("repoURLForReference: " + repoURLForReference);
            }

        }

        return repoURLForReference;
    }

    public void setRepoURLToTest(String repoURLToTest) {
        this.repoURLToTest = repoURLToTest;
    }

    public void setRepoURLForReference(String repoURLForReference) {
        this.repoURLForReference = repoURLForReference;
    }

    protected boolean isCategory(IInstallableUnit curiu) {
        // ignore categories
        boolean isCategory = "true".equals(curiu.getProperty("org.eclipse.equinox.p2.type.category"));
        return isCategory;
    }
}
