package org.eclipse.cbi.p2repo.analyzers.reports;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.function.Consumer;
import org.eclipse.cbi.p2repo.analyzers.common.CheckReport;
import org.eclipse.cbi.p2repo.analyzers.common.ReportType;
import org.eclipse.cbi.p2repo.analyzers.common.reporter.CheckReportsManager;
import org.eclipse.cbi.p2repo.analyzers.common.reporter.ICheckReporter;
import org.eclipse.cbi.p2repo.analyzers.common.reporter.IP2RepositoryAnalyserConfiguration;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

/**
 * @author dhuebner - Initial contribution and API
 */
@SuppressWarnings("all")
public class OverviewReport implements ICheckReporter {
  @Override
  public void createReport(final CheckReportsManager manager, final IP2RepositoryAnalyserConfiguration configs) {
    try {
      String _dataOutputDir = configs.getDataOutputDir();
      String _plus = (_dataOutputDir + "/overview.csv");
      final PrintWriter writer = new PrintWriter(_plus);
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("IU id;IU version;ReportType;Checker Class;Checker output;Checker additional data;Report creation Time");
      writer.println(_builder);
      final Function1<CheckReport, String> _function = (CheckReport it) -> {
        return it.getIU().getId();
      };
      final Comparator<CheckReport> _function_1 = (CheckReport $0, CheckReport $1) -> {
        return Integer.valueOf($1.getType().ordinal()).compareTo(Integer.valueOf($0.getType().ordinal()));
      };
      final Consumer<CheckReport> _function_2 = (CheckReport it) -> {
        StringConcatenation _builder_1 = new StringConcatenation();
        String _id = it.getIU().getId();
        _builder_1.append(_id);
        _builder_1.append(";");
        String _iuVersion = it.getIuVersion();
        _builder_1.append(_iuVersion);
        _builder_1.append(";");
        ReportType _type = it.getType();
        _builder_1.append(_type);
        _builder_1.append(";");
        String _checkerId = it.getCheckerId();
        _builder_1.append(_checkerId);
        _builder_1.append(";");
        String _checkResult = it.getCheckResult();
        _builder_1.append(_checkResult);
        _builder_1.append(";");
        String _additionalData = it.getAdditionalData();
        _builder_1.append(_additionalData);
        _builder_1.append(";");
        long _timeMs = it.getTimeMs();
        _builder_1.append(_timeMs);
        writer.println(_builder_1);
      };
      IterableExtensions.<CheckReport>sortWith(IterableExtensions.<CheckReport, String>sortBy(manager.getReports(), _function), _function_1).forEach(_function_2);
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
