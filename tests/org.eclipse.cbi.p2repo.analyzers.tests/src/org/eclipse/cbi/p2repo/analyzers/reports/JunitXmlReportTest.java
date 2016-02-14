package org.eclipse.cbi.p2repo.analyzers.reports;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author dhuebner test some utilities
 *
 */
public class JunitXmlReportTest {

	@Test
	public void testFormatting() {
		JunitXmlReport junitXmlReport = new JunitXmlReport();
		assertEquals("Format 0 size", "0.000", junitXmlReport.toTimeFormat(0));
		assertEquals("Format negative size", "0.000", junitXmlReport.toTimeFormat(-1));
		assertEquals("Format 1 size", "0.001", junitXmlReport.toTimeFormat(1));
		assertEquals("Format 1000 size", "1.000", junitXmlReport.toTimeFormat(1000));
		assertEquals("Format 10000000 size", "10000.000", junitXmlReport.toTimeFormat(10000000));
		assertEquals("Format 99999999 size", "99999.999", junitXmlReport.toTimeFormat(99999999));
	}

}
