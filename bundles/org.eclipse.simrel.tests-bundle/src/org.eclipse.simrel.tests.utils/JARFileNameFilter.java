package org.eclipse.simrel.tests.utils;

import java.io.File;
import java.io.FilenameFilter;

public class JARFileNameFilter implements FilenameFilter {
    private static final String EXTENSION_JAR = ".jar";

    public boolean accept(File dir, String name) {
        return name.endsWith(EXTENSION_JAR);
    }
}
