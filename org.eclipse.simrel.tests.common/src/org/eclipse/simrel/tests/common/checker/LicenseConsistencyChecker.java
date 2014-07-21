/**
 * 
 */
package org.eclipse.simrel.tests.common.checker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Consumer;

import org.eclipse.equinox.internal.p2.metadata.License;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ILicense;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.P2RepositoryDescription;
import org.eclipse.simrel.tests.common.utils.IUUtil;

/**
 * @author dhuebner
 *
 */
@SuppressWarnings("restriction")
public class LicenseConsistencyChecker implements IUChecker {
	private static String STANDARD_LICENSES_PROPERTIES_FILE = "standardLicenses.properties";
	private License standardLicense2010;
	private License standardLicense2011;
	private License standardLicense2014;

	public LicenseConsistencyChecker() {
		Properties properties = new Properties();

		InputStream inStream = this.getClass().getResourceAsStream(STANDARD_LICENSES_PROPERTIES_FILE);
		try {
			properties.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		String body2014 = properties.getProperty("license2014");
		String body2011 = properties.getProperty("license2011");
		String body2010 = properties.getProperty("license2010");
		standardLicense2014 = new License(null, body2014, null);
		standardLicense2011 = new License(null, body2011, null);
		standardLicense2010 = new License(null, body2010, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.simrel.tests.common.IP2RepositoryChecker#check(java.util.
	 * function.Consumer,
	 * org.eclipse.simrel.tests.common.P2RepositoryDescription,
	 * org.eclipse.equinox.p2.metadata.IInstallableUnit)
	 */
	@Override
	public void check(Consumer<? super CheckReport> consumer, P2RepositoryDescription descr, IInstallableUnit feature) {
		if (IUUtil.isFeature(feature)) {
			Collection<ILicense> licenses = feature.getLicenses(null);
			CheckReport report = new CheckReport(LicenseConsistencyChecker.class, feature);
			checkLicense(licenses, report);
			report.setTimeMs(System.currentTimeMillis());
			consumer.accept(report);
		}
	}

	private void checkLicense(Collection<ILicense> licenses, CheckReport report) {
		if (licenses.isEmpty()) {
			report.setType(ReportType.NOT_IN_TRAIN);
			report.setMessage("Licence is missing");
		} else if (licenses.size() != 1) {
			report.setType(ReportType.BAD_GUY);
			report.setMessage("Extra Licence found");
		} else {
			ILicense featureLicense = licenses.iterator().next();
			if (standardLicense2010.getUUID().equals(featureLicense.getUUID())) {
				report.setType(ReportType.BAD_GUY);
				report.setMessage("Old 2010 License");
			} else if (standardLicense2011.getUUID().equals(featureLicense.getUUID())) {
				report.setType(ReportType.BAD_GUY);
				report.setMessage("Old 2011 License");
			} else if (standardLicense2014.getUUID().equals(featureLicense.getUUID())) {
				report.setType(ReportType.INFO);
				report.setMessage("New 2014 License");
			} else {
				// if we get here, we have some kind of bad license, or its
				// missing.
				String featureLicenseText = featureLicense.getBody();
				report.setType(ReportType.NOT_IN_TRAIN);
				if (featureLicenseText == null || featureLicenseText.length() == 0) {
					report.setMessage("Missing license content");
				} else {
					// "bad" in this context means different from one of the
					// standard ones.
					report.setMessage("Not an eclipse license");
				}
			}
		}
	}

}
