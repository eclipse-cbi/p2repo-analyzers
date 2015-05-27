/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.simrel.tests.reports;

import com.google.common.base.Objects;
import com.google.common.escape.Escaper;
import com.google.common.xml.XmlEscapers;
import java.io.File;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.eclipse.simrel.tests.common.reporter.ICheckReporter;
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * @author dhuebner - Initial contribution and API
 */
@SuppressWarnings("all")
public class JunitXmlReport implements ICheckReporter {
  @Override
  public void createReport(final CheckReportsManager manager, final IP2RepositoryAnalyserConfiguration configs) {
    try {
      String _reportOutputDir = configs.getReportOutputDir();
      final File dataDir = new File(_reportOutputDir, "data");
      dataDir.mkdirs();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(dataDir, "");
      _builder.append("/junit-report.xml");
      final PrintWriter writer = new PrintWriter(_builder.toString());
      ConcurrentLinkedQueue<CheckReport> _reports = manager.getReports();
      final Function1<CheckReport, String> _function = (CheckReport it) -> {
        return it.getCheckerId();
      };
      final Map<String, List<CheckReport>> groupedByCheck = IterableExtensions.<String, CheckReport>groupBy(_reports, _function);
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      _builder_1.newLine();
      _builder_1.append("<testrun name=\"Simrel report\" project=\"org.eclipse.simrel.tests-bundle\" tests=\"");
      ConcurrentLinkedQueue<CheckReport> _reports_1 = manager.getReports();
      int _size = _reports_1.size();
      _builder_1.append(_size, "");
      _builder_1.append("\" started=\"");
      ConcurrentLinkedQueue<CheckReport> _reports_2 = manager.getReports();
      int _size_1 = _reports_2.size();
      _builder_1.append(_size_1, "");
      _builder_1.append("\" failures=\"0\" errors=\"0\" ignored=\"0\">");
      _builder_1.newLineIfNotEmpty();
      {
        Set<String> _keySet = groupedByCheck.keySet();
        for(final String check : _keySet) {
          _builder_1.append("\t");
          List<CheckReport> _get = groupedByCheck.get(check);
          final Function1<CheckReport, IInstallableUnit> _function_1 = (CheckReport it) -> {
            return it.getIU();
          };
          final Map<IInstallableUnit, List<CheckReport>> checkedIUsById = IterableExtensions.<IInstallableUnit, CheckReport>groupBy(_get, _function_1);
          _builder_1.newLineIfNotEmpty();
          _builder_1.append("\t");
          _builder_1.append("<testsuite name=\"");
          String[] _split = check.split("\\.");
          String _last = IterableExtensions.<String>last(((Iterable<String>)Conversions.doWrapArray(_split)));
          _builder_1.append(_last, "\t");
          _builder_1.append("\" time=\"");
          int _size_2 = checkedIUsById.size();
          String _timeFormat = this.toTimeFormat(Integer.valueOf(_size_2));
          _builder_1.append(_timeFormat, "\t");
          _builder_1.append("\">");
          _builder_1.newLineIfNotEmpty();
          {
            Set<IInstallableUnit> _keySet_1 = checkedIUsById.keySet();
            final Function1<IInstallableUnit, String> _function_2 = (IInstallableUnit it) -> {
              return it.getId();
            };
            List<IInstallableUnit> _sortBy = IterableExtensions.<IInstallableUnit, String>sortBy(_keySet_1, _function_2);
            for(final IInstallableUnit iu : _sortBy) {
              _builder_1.append("\t");
              _builder_1.append("\t");
              final List<CheckReport> reportsForIU = checkedIUsById.get(iu);
              _builder_1.newLineIfNotEmpty();
              _builder_1.append("\t");
              _builder_1.append("\t");
              _builder_1.append("\t");
              _builder_1.append("<testcase name=\"check_");
              String _id = iu.getId();
              _builder_1.append(_id, "\t\t\t");
              _builder_1.append("\" classname=\"");
              _builder_1.append(check, "\t\t\t");
              _builder_1.append("\" time=\"");
              int _size_3 = reportsForIU.size();
              String _timeFormat_1 = this.toTimeFormat(Integer.valueOf(_size_3));
              _builder_1.append(_timeFormat_1, "\t\t\t");
              _builder_1.append("\">");
              _builder_1.newLineIfNotEmpty();
              {
                for(final CheckReport report : reportsForIU) {
                  _builder_1.append("\t");
                  _builder_1.append("\t");
                  _builder_1.append("<");
                  ReportType _type = report.getType();
                  String _asTag = this.asTag(_type);
                  _builder_1.append(_asTag, "\t\t");
                  _builder_1.append(">");
                  _builder_1.newLineIfNotEmpty();
                  _builder_1.append("\t");
                  _builder_1.append("\t");
                  String _versionedId = report.getVersionedId();
                  _builder_1.append(_versionedId, "\t\t");
                  _builder_1.newLineIfNotEmpty();
                  {
                    String _checkResult = report.getCheckResult();
                    boolean _notEquals = (!Objects.equal(_checkResult, null));
                    if (_notEquals) {
                      _builder_1.append("\t");
                      _builder_1.append("\t");
                      Escaper _xmlAttributeEscaper = XmlEscapers.xmlAttributeEscaper();
                      String _checkResult_1 = report.getCheckResult();
                      String _escape = _xmlAttributeEscaper.escape(_checkResult_1);
                      _builder_1.append(_escape, "\t\t");
                      {
                        String _additionalData = report.getAdditionalData();
                        boolean _notEquals_1 = (!Objects.equal(_additionalData, null));
                        if (_notEquals_1) {
                          _builder_1.append(" - ");
                          Escaper _xmlAttributeEscaper_1 = XmlEscapers.xmlAttributeEscaper();
                          String _additionalData_1 = report.getAdditionalData();
                          String _escape_1 = _xmlAttributeEscaper_1.escape(_additionalData_1);
                          _builder_1.append(_escape_1, "\t\t");
                        }
                      }
                      _builder_1.newLineIfNotEmpty();
                    }
                  }
                  _builder_1.append("\t");
                  _builder_1.append("\t");
                  _builder_1.append("</");
                  ReportType _type_1 = report.getType();
                  String _asTag_1 = this.asTag(_type_1);
                  _builder_1.append(_asTag_1, "\t\t");
                  _builder_1.append(">");
                  _builder_1.newLineIfNotEmpty();
                }
              }
              _builder_1.append("\t");
              _builder_1.append("\t");
              _builder_1.append("\t");
              _builder_1.append("</testcase>");
              _builder_1.newLine();
            }
          }
          _builder_1.append("\t");
          _builder_1.append("</testsuite>");
          _builder_1.newLine();
        }
      }
      _builder_1.append("</testrun>");
      _builder_1.newLine();
      final String xmlContent = _builder_1.toString();
      writer.append(xmlContent);
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String toTimeFormat(final Integer testsCount) {
    if (((testsCount).intValue() <= 0)) {
      return "0.000";
    }
    DecimalFormatSymbols _decimalFormatSymbols = new DecimalFormatSymbols();
    final Procedure1<DecimalFormatSymbols> _function = (DecimalFormatSymbols it) -> {
      it.setDecimalSeparator('.');
    };
    final DecimalFormatSymbols decimalFormatSymbol = ObjectExtensions.<DecimalFormatSymbols>operator_doubleArrow(_decimalFormatSymbols, _function);
    final DecimalFormat customFormat = new DecimalFormat("0.000", decimalFormatSymbol);
    return customFormat.format((0.001d * (testsCount).intValue()));
  }
  
  public String asTag(final ReportType type) {
    boolean _equals = Objects.equal(type, ReportType.NOT_IN_TRAIN);
    if (_equals) {
      return "failure";
    } else {
      return "system-out";
    }
  }
}
