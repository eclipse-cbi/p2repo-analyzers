package org.eclipse.simrel.tests.utils;

import java.io.File;
import java.io.FilenameFilter;

public class PackGzFileNameFilter implements FilenameFilter {
    private static final String EXTENSION_PACEKD_JAR = ".jar.pack.gz";

    public boolean accept(File dir, String name) {
        return name.endsWith(EXTENSION_PACEKD_JAR);
    }
}
