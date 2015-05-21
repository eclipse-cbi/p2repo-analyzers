package org.eclipse.simrel.tests.reports;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.simrel.tests.common.CheckReport;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.common.reporter.CheckReportsManager;
import org.eclipse.simrel.tests.common.reporter.ICheckReporter;
import org.eclipse.simrel.tests.common.reporter.IP2RepositoryAnalyserConfiguration;
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
      _builder.append("IU id;ReportType;Checker Class;Checker output;Report creation Time");
      writer.println(_builder);
      ConcurrentLinkedQueue<CheckReport> _reports = manager.getReports();
      final Function1<CheckReport, ReportType> _function = (CheckReport it) -> {
        return it.getType();
      };
      List<CheckReport> _sortBy = IterableExtensions.<CheckReport, ReportType>sortBy(_reports, _function);
      final Function1<CheckReport, String> _function_1 = (CheckReport it) -> {
        return it.getCheckerId();
      };
      List<CheckReport> _sortBy_1 = IterableExtensions.<CheckReport, String>sortBy(_sortBy, _function_1);
      final Consumer<CheckReport> _function_2 = (CheckReport it) -> {
        StringConcatenation _builder_1 = new StringConcatenation();
        IInstallableUnit _iU = it.getIU();
        String _id = _iU.getId();
        _builder_1.append(_id, "");
        _builder_1.append(";");
        ReportType _type = it.getType();
        _builder_1.append(_type, "");
        _builder_1.append(";");
        String _checkerId = it.getCheckerId();
        _builder_1.append(_checkerId, "");
        _builder_1.append(";");
        String _checkResult = it.getCheckResult();
        _builder_1.append(_checkResult, "");
        _builder_1.append(";");
        long _timeMs = it.getTimeMs();
        _builder_1.append(_timeMs, "");
        writer.println(_builder_1);
      };
      _sortBy_1.forEach(_function_2);
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
