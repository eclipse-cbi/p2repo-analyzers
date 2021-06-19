package org.eclipse.cbi.p2repo.analyzers.repos;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

public class TestProperties {
    private static final String KNOWN_PROVIDERS_RESOURCE    = "knownProviders.properties";

    private static final String EXPECTED_PROVIDER_NAMES_KEY = "expectedProviderNames";

    private ArrayList<String>           EXPECTED_PROVIDER_NAMES     = null;

    public static void main(String[] args) {
        try {
            ArrayList<String> testnames = new TestProperties().getKnownProviderNames();
            for (String testname : testnames) {
                System.out.println(testname);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getKnownProviderNames() throws Exception {
        if (EXPECTED_PROVIDER_NAMES == null) {
            ArrayList<String> namesAsList = new ArrayList<>();
            // first try system properties, to allow override.
            String expectedProviders = System.getProperty(EXPECTED_PROVIDER_NAMES_KEY);
            if (expectedProviders == null) {
                // if no system property found, use out built-in list
                Properties names = new Properties();
                try (InputStream inStream = getClass().getResourceAsStream(KNOWN_PROVIDERS_RESOURCE)) {
                    names.load(inStream);
                    expectedProviders = names.getProperty(EXPECTED_PROVIDER_NAMES_KEY);
                    if (expectedProviders == null) {
                        throw new Exception("PROGRAM ERROR: Could not read internal property file");
                    }
                    StringTokenizer tokenizer = new StringTokenizer(expectedProviders, ",", false);
                    while (tokenizer.hasMoreTokens()) {
                        String name = tokenizer.nextToken();
                        namesAsList.add(name.trim());
                    }
                }
                EXPECTED_PROVIDER_NAMES=namesAsList;
            }
        }
        return EXPECTED_PROVIDER_NAMES;
    }
}
