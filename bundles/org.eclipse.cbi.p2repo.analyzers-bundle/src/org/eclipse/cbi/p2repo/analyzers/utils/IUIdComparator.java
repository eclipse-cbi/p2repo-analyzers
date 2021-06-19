package org.eclipse.cbi.p2repo.analyzers.utils;

import java.util.Comparator;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;

public class IUIdComparator implements Comparator<IInstallableUnit> {

    @Override
    public int compare(IInstallableUnit iu1, IInstallableUnit iu2) {
        // neither iu should be null ... but, just to cover all cases
        if ((iu1 == null) && (iu2 == null)) {
            return 0;
        } else if ((iu1 == null) || (iu2 == null)) {
            return 1;
        } else {
            return iu1.getId().compareTo(iu2.getId());
        }
    }
}