package org.eclipse.cbi.p2repo.analyzers.utils;

import java.io.File;
import java.io.FilenameFilter;

public class PackGzFileNameFilter implements FilenameFilter {
    public static final String EXTENSION_PACEKD_JAR = ".jar.pack.gz";

    public boolean accept(File dir, String name) {
        return name.endsWith(EXTENSION_PACEKD_JAR);
    }
}
