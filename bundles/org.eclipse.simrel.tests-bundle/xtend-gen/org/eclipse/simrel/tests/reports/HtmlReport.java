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
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.eclipse.simrel.tests.common.reporter.ICheckReporter;
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.StringExtensions;

/**
 * @author dhuebner - Initial contribution and API
 */
@SuppressWarnings("all")
public class HtmlReport implements ICheckReporter {
  private final String cssFileName = "html-report.css";
  
  private final String jsFileName = "html-report.js";
  
  private final String errorsFileName = "errors-and-moderate_warnings.html";
  
  private final String warningsFileName = "warnings.html";
  
  @Override
  public void createReport(final CheckReportsManager manager, final IP2RepositoryAnalyserConfiguration configs) {
    try {
      String _errorsHtmlLocation = this.errorsHtmlLocation(configs);
      final PrintWriter writer = new PrintWriter(_errorsHtmlLocation);
      final ConcurrentLinkedQueue<CheckReport> allreports = manager.getReports();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("<html>");
      _builder.newLine();
      _builder.append("<head>");
      _builder.newLine();
      _builder.append("<link rel=\"stylesheet\" href=\"./data/");
      _builder.append(this.cssFileName, "");
      _builder.append("\"/>");
      _builder.newLineIfNotEmpty();
      _builder.append("<script src=\"http://code.jquery.com/jquery-1.11.3.min.js\"></script>");
      _builder.newLine();
      _builder.append("<script src=\"./data/");
      _builder.append(this.jsFileName, "");
      _builder.append("\"></script>");
      _builder.newLineIfNotEmpty();
      _builder.append("</head>");
      _builder.newLine();
      _builder.append("<body>");
      _builder.newLine();
      _builder.append("\t");
      CharSequence _summary = this.summary(configs);
      _builder.append(_summary, "\t");
      _builder.append("<br>");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("<a href=\"./");
      _builder.append(this.warningsFileName, "\t");
      _builder.append("\">show warnings</a>");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      String _htmlTable = this.htmlTable(ReportType.NOT_IN_TRAIN, allreports);
      _builder.append(_htmlTable, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("<br>");
      _builder.newLine();
      _builder.append("\t");
      String _htmlTable_1 = this.htmlTable(ReportType.BAD_GUY, allreports);
      _builder.append(_htmlTable_1, "\t");
      _builder.newLineIfNotEmpty();
      _builder.append("</body>");
      _builder.newLine();
      _builder.append("</html>");
      _builder.newLine();
      final String xmlContent = _builder.toString();
      PrintWriter _append = writer.append(xmlContent);
      _append.close();
      String _warningsHtmlLocation = this.warningsHtmlLocation(configs);
      final PrintWriter warnWriter = new PrintWriter(_warningsHtmlLocation);
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("<html>");
      _builder_1.newLine();
      _builder_1.append("<head>");
      _builder_1.newLine();
      _builder_1.append("<link rel=\"stylesheet\" href=\"./data/");
      _builder_1.append(this.cssFileName, "");
      _builder_1.append("\"/>");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("<script src=\"http://code.jquery.com/jquery-1.11.3.min.js\"></script>");
      _builder_1.newLine();
      _builder_1.append("<script src=\"./data/");
      _builder_1.append(this.jsFileName, "");
      _builder_1.append("\"></script>");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("</head>");
      _builder_1.newLine();
      _builder_1.append("<body>");
      _builder_1.newLine();
      _builder_1.append("\t");
      CharSequence _summary_1 = this.summary(configs);
      _builder_1.append(_summary_1, "\t");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("\t");
      _builder_1.append("<a href=\"./");
      _builder_1.append(this.errorsFileName, "\t");
      _builder_1.append("\">show errors</a>");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("\t");
      String _htmlTable_2 = this.htmlTable(ReportType.WARNING, allreports);
      _builder_1.append(_htmlTable_2, "\t");
      _builder_1.newLineIfNotEmpty();
      _builder_1.append("</body>");
      _builder_1.newLine();
      _builder_1.append("</html>");
      _builder_1.newLine();
      final String warnContent = _builder_1.toString();
      PrintWriter _append_1 = warnWriter.append(warnContent);
      _append_1.close();
      this.addCssFile(configs);
      this.addJsFile(configs);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String errorsHtmlLocation(final IP2RepositoryAnalyserConfiguration configs) {
    StringConcatenation _builder = new StringConcatenation();
    String _reportOutputDir = configs.getReportOutputDir();
    _builder.append(_reportOutputDir, "");
    _builder.append("/");
    _builder.append(this.errorsFileName, "");
    return _builder.toString();
  }
  
  public String warningsHtmlLocation(final IP2RepositoryAnalyserConfiguration configs) {
    StringConcatenation _builder = new StringConcatenation();
    String _reportOutputDir = configs.getReportOutputDir();
    _builder.append(_reportOutputDir, "");
    _builder.append("/");
    _builder.append(this.warningsFileName, "");
    return _builder.toString();
  }
  
  public CharSequence summary(final IP2RepositoryAnalyserConfiguration conf) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<h3>Check results for the repository: ");
    URI _reportRepoURI = conf.getReportRepoURI();
    _builder.append(_reportRepoURI, "");
    _builder.append("</h3>");
    _builder.newLineIfNotEmpty();
    return _builder;
  }
  
  public String htmlTable(final ReportType reportType, final Iterable<CheckReport> allreports) {
    final Function1<CheckReport, Boolean> _function = (CheckReport it) -> {
      ReportType _type = it.getType();
      return Boolean.valueOf(Objects.equal(_type, reportType));
    };
    final Iterable<CheckReport> reports = IterableExtensions.<CheckReport>filter(allreports, _function);
    final Function1<CheckReport, IInstallableUnit> _function_1 = (CheckReport it) -> {
      return it.getIU();
    };
    final Map<IInstallableUnit, List<CheckReport>> groupbyIU = IterableExtensions.<IInstallableUnit, CheckReport>groupBy(reports, _function_1);
    final Function1<CheckReport, String> _function_2 = (CheckReport it) -> {
      return it.getCheckerId();
    };
    Iterable<String> _map = IterableExtensions.<CheckReport, String>map(allreports, _function_2);
    Set<String> _set = IterableExtensions.<String>toSet(_map);
    final List<String> checkerIds = IterableExtensions.<String>sort(_set);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("<h3 class=\"");
    String _asCssClass = this.asCssClass(reportType);
    _builder.append(_asCssClass, "");
    _builder.append("\">Installation units with ");
    String _asHeaderTitle = this.asHeaderTitle(reportType);
    _builder.append(_asHeaderTitle, "");
    _builder.append("s (");
    int _size = IterableExtensions.size(reports);
    _builder.append(_size, "");
    _builder.append(")</h3>");
    _builder.newLineIfNotEmpty();
    _builder.newLine();
    _builder.append("<table id=\"table_");
    String _asCssClass_1 = this.asCssClass(reportType);
    _builder.append(_asCssClass_1, "");
    _builder.append("\">");
    _builder.newLineIfNotEmpty();
    _builder.append("\t");
    _builder.append("<thead>");
    _builder.newLine();
    _builder.append("\t\t");
    _builder.append("<tr>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<td>Id</td>");
    _builder.newLine();
    _builder.append("\t\t\t");
    _builder.append("<td>Version</td>");
    _builder.newLine();
    {
      for(final String checker : checkerIds) {
        _builder.append("\t\t\t");
        _builder.append("<td title=\"");
        _builder.append(checker, "\t\t\t");
        _builder.append("\">");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        String _abbreviation = this.abbreviation(checker);
        _builder.append(_abbreviation, "\t\t\t");
        _builder.append("&nbsp;");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("<input type=\"checkbox\" name=\"checker\" class=\"");
        String _asCssClass_2 = this.asCssClass(reportType);
        _builder.append(_asCssClass_2, "\t\t\t");
        _builder.append("_toggler\" checker=\"");
        String _abbreviation_1 = this.abbreviation(checker);
        _builder.append(_abbreviation_1, "\t\t\t");
        _builder.append("\" checked=\"true\">");
        _builder.newLineIfNotEmpty();
        _builder.append("\t\t\t");
        _builder.append("</td>\t");
        _builder.newLine();
      }
    }
    _builder.append("\t\t");
    _builder.append("</tr>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("</thead>");
    _builder.newLine();
    _builder.append("\t");
    _builder.append("<tbody>");
    _builder.newLine();
    {
      Set<IInstallableUnit> _keySet = groupbyIU.keySet();
      final Function1<IInstallableUnit, String> _function_3 = (IInstallableUnit it) -> {
        return it.getId();
      };
      List<IInstallableUnit> _sortBy = IterableExtensions.<IInstallableUnit, String>sortBy(_keySet, _function_3);
      for(final IInstallableUnit iu : _sortBy) {
        _builder.append("\t");
        final Function1<CheckReport, Boolean> _function_4 = (CheckReport it) -> {
          IInstallableUnit _iU = it.getIU();
          return Boolean.valueOf(Objects.equal(_iU, iu));
        };
        final Iterable<CheckReport> iuReports = IterableExtensions.<CheckReport>filter(allreports, _function_4);
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("<tr class=\"");
        String _id = iu.getId();
        _builder.append(_id, "\t");
        _builder.append("_");
        Version _version = iu.getVersion();
        String _original = _version.getOriginal();
        _builder.append(_original, "\t");
        _builder.append("\">");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("\t");
        _builder.append("<td>");
        String _id_1 = iu.getId();
        _builder.append(_id_1, "\t\t");
        _builder.append("</td>");
        _builder.newLineIfNotEmpty();
        _builder.append("\t");
        _builder.append("\t");
        _builder.append("<td>");
        Version _version_1 = iu.getVersion();
        String _original_1 = _version_1.getOriginal();
        _builder.append(_original_1, "\t\t");
        _builder.append("</td>");
        _builder.newLineIfNotEmpty();
        {
          for(final String checker_1 : checkerIds) {
            _builder.append("\t");
            _builder.append("\t");
            final Function1<CheckReport, Boolean> _function_5 = (CheckReport it) -> {
              String _checkerId = it.getCheckerId();
              return Boolean.valueOf(Objects.equal(_checkerId, checker_1));
            };
            Iterable<CheckReport> _filter = IterableExtensions.<CheckReport>filter(iuReports, _function_5);
            final CheckReport report = IterableExtensions.<CheckReport>head(_filter);
            _builder.newLineIfNotEmpty();
            _builder.append("\t");
            _builder.append("\t");
            _builder.append("<td title=\"");
            String _asDescription = this.asDescription(report);
            _builder.append(_asDescription, "\t\t");
            _builder.append("\" class=\"");
            String _asCssClass_3 = this.asCssClass(report);
            _builder.append(_asCssClass_3, "\t\t");
            _builder.append("\" data-result=\"");
            String _asCssClass_4 = this.asCssClass(reportType);
            _builder.append(_asCssClass_4, "\t\t");
            _builder.append("_");
            String _asCssClass_5 = this.asCssClass(report);
            _builder.append(_asCssClass_5, "\t\t");
            _builder.append("_");
            String _abbreviation_2 = this.abbreviation(checker_1);
            _builder.append(_abbreviation_2, "\t\t");
            _builder.append("\" data-checker=\"");
            String _abbreviation_3 = this.abbreviation(checker_1);
            _builder.append(_abbreviation_3, "\t\t");
            _builder.append("\">");
            String _asStatus = this.asStatus(report);
            _builder.append(_asStatus, "\t\t");
            _builder.append("</td>\t");
            _builder.newLineIfNotEmpty();
          }
        }
        _builder.append("\t");
        _builder.append("</tr>");
        _builder.newLine();
      }
    }
    _builder.append("\t");
    _builder.append("</tbody>");
    _builder.newLine();
    _builder.append("</table>");
    _builder.newLine();
    final String html = _builder.toString();
    return html;
  }
  
  public void addJsFile(final IP2RepositoryAnalyserConfiguration configs) {
    try {
      StringConcatenation _builder = new StringConcatenation();
      String _dataOutputDir = configs.getDataOutputDir();
      _builder.append(_dataOutputDir, "");
      _builder.append("/");
      _builder.append(this.jsFileName, "");
      final PrintWriter writer = new PrintWriter(_builder.toString());
      final ReportType[] types = ReportType.values();
      for (final ReportType type : types) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("$(document).ready(function() {");
        _builder_1.newLine();
        _builder_1.append("\t");
        _builder_1.append("$(\".");
        String _asCssClass = this.asCssClass(type);
        _builder_1.append(_asCssClass, "\t");
        _builder_1.append("_toggler\").click(function(e) {");
        _builder_1.newLineIfNotEmpty();
        _builder_1.append("\t\t");
        _builder_1.append("var targets = $(\'*[data-result=\"");
        String _asCssClass_1 = this.asCssClass(type);
        _builder_1.append(_asCssClass_1, "\t\t");
        _builder_1.append("_");
        String _asCssClass_2 = this.asCssClass(type);
        _builder_1.append(_asCssClass_2, "\t\t");
        _builder_1.append("_\' + $(this).attr(\'checker\')+\'\"]\');");
        _builder_1.newLineIfNotEmpty();
        _builder_1.append("\t\t");
        _builder_1.append("var state = $(this).attr(\'checked\')");
        _builder_1.newLine();
        _builder_1.append("\t\t");
        _builder_1.append("targets.each(function() {");
        _builder_1.newLine();
        _builder_1.append("\t\t\t");
        _builder_1.append("var tr = $(this).parent();");
        _builder_1.newLine();
        _builder_1.append("\t\t\t");
        _builder_1.append("tr.toggle(state)");
        _builder_1.newLine();
        _builder_1.append("\t\t");
        _builder_1.append("});");
        _builder_1.newLine();
        _builder_1.append("\t");
        _builder_1.append("});");
        _builder_1.newLine();
        _builder_1.append("});");
        _builder_1.newLine();
        writer.append(_builder_1);
      }
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public void addCssFile(final IP2RepositoryAnalyserConfiguration configs) {
    try {
      StringConcatenation _builder = new StringConcatenation();
      String _dataOutputDir = configs.getDataOutputDir();
      _builder.append(_dataOutputDir, "");
      _builder.append("/");
      _builder.append(this.cssFileName, "");
      final PrintWriter writer = new PrintWriter(_builder.toString());
      StringConcatenation _builder_1 = new StringConcatenation();
      _builder_1.append("table {");
      _builder_1.newLine();
      _builder_1.append("\t");
      _builder_1.append("min-width: 79%;");
      _builder_1.newLine();
      _builder_1.append("}");
      _builder_1.newLine();
      _builder_1.append("thead {");
      _builder_1.newLine();
      _builder_1.append("    ");
      _builder_1.append("padding: 2px;");
      _builder_1.newLine();
      _builder_1.append("    ");
      _builder_1.append("background-color: #E8E8E8;");
      _builder_1.newLine();
      _builder_1.append("}");
      _builder_1.newLine();
      _builder_1.append("td {");
      _builder_1.newLine();
      _builder_1.append("    ");
      _builder_1.append("padding: 2px;");
      _builder_1.newLine();
      _builder_1.append("}");
      _builder_1.newLine();
      writer.append(_builder_1);
      ReportType[] _values = ReportType.values();
      for (final ReportType type : _values) {
        StringConcatenation _builder_2 = new StringConcatenation();
        _builder_2.append(".");
        String _asCssClass = this.asCssClass(type);
        _builder_2.append(_asCssClass, "");
        _builder_2.append(" {");
        _builder_2.newLineIfNotEmpty();
        _builder_2.append("\t");
        _builder_2.append("text-align: center;");
        _builder_2.newLine();
        _builder_2.append("\t");
        _builder_2.append("background-color: #");
        String _asBgColor = this.asBgColor(type);
        _builder_2.append(_asBgColor, "\t");
        _builder_2.append(";");
        _builder_2.newLineIfNotEmpty();
        _builder_2.append("}");
        _builder_2.newLine();
        writer.append(_builder_2);
      }
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String asBgColor(final ReportType type) {
    String _switchResult = null;
    if (type != null) {
      switch (type) {
        case NOT_IN_TRAIN:
          _switchResult = "FFCCCC";
          break;
        case BAD_GUY:
          _switchResult = "FFCC66";
          break;
        case WARNING:
          _switchResult = "FFFFCC";
          break;
        case INFO:
          _switchResult = "CCFFCC";
          break;
        default:
          break;
      }
    }
    return _switchResult;
  }
  
  public String asCssClass(final CheckReport report) {
    String _xblockexpression = null;
    {
      boolean _equals = Objects.equal(report, null);
      if (_equals) {
        return "skipped_check";
      }
      ReportType _type = report.getType();
      _xblockexpression = this.asCssClass(_type);
    }
    return _xblockexpression;
  }
  
  public String asCssClass(final ReportType type) {
    String _switchResult = null;
    if (type != null) {
      switch (type) {
        case NOT_IN_TRAIN:
          _switchResult = "error_result";
          break;
        case BAD_GUY:
          _switchResult = "moderate_warning_result";
          break;
        case WARNING:
          _switchResult = "warning_result";
          break;
        case INFO:
          _switchResult = "info_result";
          break;
        default:
          break;
      }
    }
    return _switchResult;
  }
  
  public String asHeaderTitle(final ReportType type) {
    String _switchResult = null;
    if (type != null) {
      switch (type) {
        case NOT_IN_TRAIN:
          _switchResult = "Error";
          break;
        case BAD_GUY:
          _switchResult = "Moderate Warning";
          break;
        case WARNING:
          _switchResult = "Warning";
          break;
        case INFO:
          _switchResult = "Info";
          break;
        default:
          break;
      }
    }
    return _switchResult;
  }
  
  public String abbreviation(final String string) {
    String simpleName = string;
    final int dotIndex = string.lastIndexOf(".");
    boolean _and = false;
    if (!(dotIndex >= 0)) {
      _and = false;
    } else {
      int _length = string.length();
      boolean _lessThan = (dotIndex < _length);
      _and = _lessThan;
    }
    if (_and) {
      String _substring = string.substring((dotIndex + 1));
      simpleName = _substring;
    }
    return simpleName.replaceAll("([A-Z]+)((?![A-Z])\\w)+", "$1");
  }
  
  public String asDescription(final CheckReport report) {
    boolean _equals = Objects.equal(report, null);
    if (_equals) {
      return "any reports";
    } else {
      String _xifexpression = null;
      String _checkResult = report.getCheckResult();
      boolean _equals_1 = Objects.equal(_checkResult, null);
      if (_equals_1) {
        _xifexpression = "null";
      } else {
        _xifexpression = report.getCheckResult();
      }
      final String result = _xifexpression;
      StringConcatenation _builder = new StringConcatenation();
      _builder.append(result, "");
      String _xifexpression_1 = null;
      String _additionalData = report.getAdditionalData();
      boolean _isNullOrEmpty = StringExtensions.isNullOrEmpty(_additionalData);
      boolean _not = (!_isNullOrEmpty);
      if (_not) {
        String _additionalData_1 = report.getAdditionalData();
        _xifexpression_1 = (" - " + _additionalData_1);
      }
      _builder.append(_xifexpression_1, "");
      return _builder.toString();
    }
  }
  
  public String asStatus(final CheckReport report) {
    String _xblockexpression = null;
    {
      boolean _equals = Objects.equal(report, null);
      if (_equals) {
        return "&nbsp;";
      }
      String _switchResult = null;
      ReportType _type = report.getType();
      if (_type != null) {
        switch (_type) {
          case NOT_IN_TRAIN:
            _switchResult = "--";
            break;
          case BAD_GUY:
            _switchResult = "-";
            break;
          case WARNING:
            _switchResult = "+";
            break;
          case INFO:
            _switchResult = "++";
            break;
          default:
            break;
        }
      }
      _xblockexpression = _switchResult;
    }
    return _xblockexpression;
  }
}
