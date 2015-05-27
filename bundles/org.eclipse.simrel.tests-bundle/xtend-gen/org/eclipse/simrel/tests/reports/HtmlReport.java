/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.simrel.tests.reports;

import com.google.common.base.Objects;
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
public class HtmlReport implements ICheckReporter {
  @Override
  public void createReport(final CheckReportsManager manager, final IP2RepositoryAnalyserConfiguration configs) {
    try {
      StringConcatenation _builder = new StringConcatenation();
      String _reportOutputDir = configs.getReportOutputDir();
      _builder.append(_reportOutputDir, "");
      _builder.append("/errors-and-warnings.html");
      final PrintWriter writer = new PrintWriter(_builder.toString());
      final ConcurrentLinkedQueue<CheckReport> allreports = manager.getReports();
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("<html>");
      _builder_1.newLine();
      _builder_1.append("<body>");
      _builder_1.newLine();
      final Function1<CheckReport, Boolean> _function = (CheckReport it) -> {
        ReportType _type = it.getType();
        return Boolean.valueOf(Objects.equal(_type, ReportType.NOT_IN_TRAIN));
      };
      final Iterable<CheckReport> errors = IterableExtensions.<CheckReport>filter(allreports, _function);
      _builder_1.newLineIfNotEmpty();
      {
        boolean _isEmpty = IterableExtensions.isEmpty(errors);
        boolean _not = (!_isEmpty);
        if (_not) {
          _builder_1.append("<b>Errors</b><br>");
          _builder_1.newLine();
          String _htmlTable = this.htmlTable(errors, allreports);
          _builder_1.append(_htmlTable, "");
          _builder_1.newLineIfNotEmpty();
        }
      }
      _builder_1.append("\t");
      _builder_1.append("<b>Warnings</b><br>");
      _builder_1.newLine();
      final Function1<CheckReport, Boolean> _function_1 = (CheckReport it) -> {
        ReportType _type = it.getType();
        return Boolean.valueOf(Objects.equal(_type, ReportType.BAD_GUY));
      };
      final Iterable<CheckReport> warnings = IterableExtensions.<CheckReport>filter(allreports, _function_1);
      _builder_1.newLineIfNotEmpty();
      String _htmlTable_1 = this.htmlTable(warnings, allreports);
      _builder_1.append(_htmlTable_1, "");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("</body>");
      _builder_1.newLine();
      _builder_1.append("</html>");
      _builder_1.newLine();
      final String xmlContent = _builder_1.toString();
      writer.append(xmlContent);
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String htmlTable(final Iterable<CheckReport> reports, final Iterable<CheckReport> allreports) {
    final Function1<CheckReport, IInstallableUnit> _function = (CheckReport it) -> {
      return it.getIU();
    };
    final Map<IInstallableUnit, List<CheckReport>> groupbyIU = IterableExtensions.<IInstallableUnit, CheckReport>groupBy(reports, _function);
    final Function1<CheckReport, String> _function_1 = (CheckReport it) -> {
      return it.getCheckerId();
    };
    Iterable<String> _map = IterableExtensions.<CheckReport, String>map(allreports, _function_1);
    Set<String> _set = IterableExtensions.<String>toSet(_map);
    final List<String> checkerIds = IterableExtensions.<String>sort(_set);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<table>");
    _builder.newLine();
    _builder.append("\t");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<tr>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<td>IU Id</td>");
    _builder.newLine();
    {
      for(final String checker : checkerIds) {
        _builder.append("\t\t");
        _builder.append("<td>");
        String[] _split = checker.split("\\.");
        String _last = IterableExtensions.<String>last(((Iterable<String>)Conversions.doWrapArray(_split)));
        _builder.append(_last, "\t\t");
        _builder.append("</td>\t");
        _builder.newLineIfNotEmpty();
      }
    }
    _builder.append("\t");
    _builder.append("</tr>");
    _builder.newLine();
    {
      Set<IInstallableUnit> _keySet = groupbyIU.keySet();
      final Function1<IInstallableUnit, String> _function_2 = (IInstallableUnit it) -> {
        return it.getId();
      };
      List<IInstallableUnit> _sortBy = IterableExtensions.<IInstallableUnit, String>sortBy(_keySet, _function_2);
      for(final IInstallableUnit iu : _sortBy) {
        final Function1<CheckReport, Boolean> _function_3 = (CheckReport it) -> {
          IInstallableUnit _iU = it.getIU();
          return Boolean.valueOf(Objects.equal(_iU, iu));
        };
        final Iterable<CheckReport> iuReports = IterableExtensions.<CheckReport>filter(allreports, _function_3);
        _builder.newLineIfNotEmpty();
        _builder.append("<tr>");
        _builder.newLine();
        _builder.append("\t");
        _builder.append("<td>");
        String _id = iu.getId();
        _builder.append(_id, "\t");
        _builder.append("</td>");
        _builder.newLineIfNotEmpty();
        {
          for(final String checker_1 : checkerIds) {
            _builder.append("\t");
            _builder.append("<td>");
            final Function1<CheckReport, Boolean> _function_4 = (CheckReport it) -> {
              String _checkerId = it.getCheckerId();
              return Boolean.valueOf(Objects.equal(_checkerId, checker_1));
            };
            Iterable<CheckReport> _filter = IterableExtensions.<CheckReport>filter(iuReports, _function_4);
            final Function1<CheckReport, String> _function_5 = (CheckReport it) -> {
              return this.asStatus(it);
            };
            Iterable<String> _map_1 = IterableExtensions.<CheckReport, String>map(_filter, _function_5);
            String _join = IterableExtensions.join(_map_1, ",");
            _builder.append(_join, "\t");
            _builder.append("</td>\t");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("</tr>");
        _builder.newLine();
      }
    }
    _builder.append("</table>");
    _builder.newLine();
    final String html = _builder.toString();
    return html;
  }
  
  public String asStatus(final CheckReport report) {
    String _switchResult = null;
    ReportType _type = report.getType();
    if (_type != null) {
      switch (_type) {
        case NOT_IN_TRAIN:
          _switchResult = "_";
          break;
        case BAD_GUY:
          _switchResult = "0";
          break;
        case INFO:
          _switchResult = "+";
          break;
        default:
          _switchResult = "";
          break;
      }
    } else {
      _switchResult = "";
    }
    return _switchResult;
  }
}
