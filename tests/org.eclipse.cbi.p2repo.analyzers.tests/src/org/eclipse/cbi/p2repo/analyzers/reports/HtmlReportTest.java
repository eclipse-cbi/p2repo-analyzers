package org.eclipse.cbi.p2repo.analyzers.reports;

import static org.junit.Assert.*;

import org.junit.Test;

public class HtmlReportTest {

	@Test
	public void testAbbreviation() {
		HtmlReport reporter = new HtmlReport();
		assertEquals("CCW", reporter.abbreviation("CamleCaseWord"));
		assertEquals("HTMLR", reporter.abbreviation("HTMLReporter"));
		assertEquals("HR", reporter.abbreviation("org.eclipse.cbi.p2repo.analyzers.reports.HtmlReport"));
		assertEquals("HR", reporter.abbreviation(".HtmlReport"));
		assertEquals("", reporter.abbreviation("HtmlReport."));
	}

}
