/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.simrel.tests.reports;

import com.google.common.base.Objects;
import java.io.File;
import java.io.PrintWriter;
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
      final Function1<CheckReport, IInstallableUnit> _function = (CheckReport it) -> {
        return it.getIU();
      };
      final Map<IInstallableUnit, List<CheckReport>> groupedByIU = IterableExtensions.<IInstallableUnit, CheckReport>groupBy(_reports, _function);
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
        Set<IInstallableUnit> _keySet = groupedByIU.keySet();
        for(final IInstallableUnit iu : _keySet) {
          _builder_1.append("\t");
          _builder_1.append("<testsuite name=\"");
          String _id = iu.getId();
          _builder_1.append(_id, "\t");
          _builder_1.append("\" time=\"0.001\">");
          _builder_1.newLineIfNotEmpty();
          _builder_1.append("\t");
          _builder_1.append("\t");
          List<CheckReport> _get = groupedByIU.get(iu);
          final Function1<CheckReport, String> _function_1 = (CheckReport it) -> {
            return it.getCheckerId();
          };
          final Map<String, List<CheckReport>> groupByChecker = IterableExtensions.<String, CheckReport>groupBy(_get, _function_1);
          _builder_1.newLineIfNotEmpty();
          {
            Set<String> _keySet_1 = groupByChecker.keySet();
            for(final String checker : _keySet_1) {
              _builder_1.append("\t");
              _builder_1.append("\t");
              _builder_1.append("<testcase name=\"check");
              String[] _split = checker.split("\\.");
              String _last = IterableExtensions.<String>last(((Iterable<String>)Conversions.doWrapArray(_split)));
              _builder_1.append(_last, "\t\t");
              _builder_1.append("\" classname=\"");
              _builder_1.append(checker, "\t\t");
              _builder_1.append("\" time=\"0.0\">");
              _builder_1.newLineIfNotEmpty();
              {
                List<CheckReport> _get_1 = groupByChecker.get(checker);
                final Function1<CheckReport, Boolean> _function_2 = (CheckReport it) -> {
                  ReportType _type = it.getType();
                  return Boolean.valueOf(Objects.equal(_type, ReportType.NOT_IN_TRAIN));
                };
                Iterable<CheckReport> _filter = IterableExtensions.<CheckReport>filter(_get_1, _function_2);
                int _size_2 = IterableExtensions.size(_filter);
                boolean _greaterThan = (_size_2 > 0);
                if (_greaterThan) {
                  _builder_1.append("\t");
                  _builder_1.append("\t");
                  _builder_1.append("<failure>");
                  _builder_1.newLine();
                  {
                    List<CheckReport> _get_2 = groupByChecker.get(checker);
                    final Function1<CheckReport, Boolean> _function_3 = (CheckReport it) -> {
                      ReportType _type = it.getType();
                      return Boolean.valueOf(Objects.equal(_type, ReportType.NOT_IN_TRAIN));
                    };
                    Iterable<CheckReport> _filter_1 = IterableExtensions.<CheckReport>filter(_get_2, _function_3);
                    for(final CheckReport error : _filter_1) {
                      _builder_1.append("\t");
                      _builder_1.append("\t");
                      String _checkResult = error.getCheckResult();
                      _builder_1.append(_checkResult, "\t\t");
                      _builder_1.append(" reported by: ");
                      String _checkerId = error.getCheckerId();
                      _builder_1.append(_checkerId, "\t\t");
                      _builder_1.newLineIfNotEmpty();
                    }
                  }
                  _builder_1.append("\t");
                  _builder_1.append("\t");
                  _builder_1.append("</failure>");
                  _builder_1.newLine();
                }
              }
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
}
