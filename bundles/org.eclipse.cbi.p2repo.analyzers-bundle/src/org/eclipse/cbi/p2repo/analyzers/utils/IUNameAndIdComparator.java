package org.eclipse.cbi.p2repo.analyzers.utils;

import java.util.Comparator;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUNameAndIdComparator implements Comparator<IInstallableUnit> {

    public int compare(IInstallableUnit iu1, IInstallableUnit iu2) {
        // neither iu should be null ... but, just to cover all cases
        if ((iu1 == null) && (iu2 == null)) {
            return 0;
        } else if ((iu1 == null) || (iu2 == null)) {
            return 1;
        } else {
            String p1 = iu1.getProperty(IInstallableUnit.PROP_NAME, null);
            String p2 = iu2.getProperty(IInstallableUnit.PROP_NAME, null);
            if (p1 == null) {
                p1 = "null";
            }
            if (p2 == null) {
                p2 = "null";
            }
            int result = p1.compareTo(p2);
            if (result == 0) {
                // if provider names are equal, use id as secondary
                // sort key
                result = iu1.getId().compareTo(iu2.getId());
            }
            return result;
        }
    }
}