/**
 *
 */
package org.eclipse.cbi.p2repo.analyzers.utils;

import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;

/**
 * @author dhuebner
 *
 */
public class PlainCheckReport extends CheckReport {

    private String iuType;
    private String fileName;

    public PlainCheckReport(Class<?> checkerId, IInstallableUnit iu) {
        super(checkerId, iu);
    }

    public PlainCheckReport() {
        this(PlainCheckReport.class, null);
    }

    /**
     * @return the iuType
     */
    public String getIuType() {
        return iuType;
    }

    /**
     * @param iuType
     *            the iuType to set
     */
    public void setIuType(String iuType) {
        this.iuType = iuType;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
