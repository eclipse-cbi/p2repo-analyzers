/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 *******************************************************************************/

package org.eclipse.cbi.p2repo.analyzers.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FullJarNameParser {

    private static final boolean DEBUG          = false;
    // simplified pattern: (ID) '_' (N '.' M '.' O '.' S) '.jar'
    private String               START_GROUP    = "(";
    private String               END_GROUP      = ")";
    private String               UNDERSCORE     = "_";
    private String               BACKSLASH      = "\\";
    private String               LITERAL_PERIOD = BACKSLASH + ".";
    private String               ANYDIGITS      = BACKSLASH + "d" + "*";
    private String               ANY            = ".*";
    private Pattern              pattern        = Pattern.compile(START_GROUP + ANY + END_GROUP + UNDERSCORE + START_GROUP
                                                        + START_GROUP + ANYDIGITS + END_GROUP + LITERAL_PERIOD + START_GROUP
                                                        + ANYDIGITS + END_GROUP + LITERAL_PERIOD + START_GROUP + ANYDIGITS
                                                        + END_GROUP + START_GROUP + LITERAL_PERIOD + ANY + END_GROUP + "?"
                                                        + END_GROUP);

    private String               projectString;
    private String               versionString;

    public boolean parse(String line) {
        boolean result = false;
        projectString = "";
        versionString = "";
        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            result = false;
            if (DEBUG) {
                System.out.println();
                System.out.println("\tthe line did not match parse rule: ");
                System.out.println("\t" + line);
                System.out.println();
            }
        } else {
            projectString = matcher.group(1);
            versionString = matcher.group(2);
            result = true;
            if (DEBUG) {
                System.out.println(projectString);
                System.out.println(versionString);
                System.out.println();
            }
        }
        return result;
    }

    public String getProjectString() {
        return projectString;
    }

    public String getVersionString() {
        return versionString;
    }
}