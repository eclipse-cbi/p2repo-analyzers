package org.eclipse.cbi.p2repo.analyzers.utils;

import java.util.Comparator;

public class StringLengthComparator implements Comparator<String> {

    public int compare(String string1, String string2) {
        if ((string1 == null) && (string2 == null)) {
            return 0;
        } else if ((string1 == null) || (string2 == null)) {
            return 1;
        } else {
            Integer length1 = Integer.valueOf(string1.length());
            Integer length2 = Integer.valueOf(string2.length());
            return length1.compareTo(length2);
        }
    }
}