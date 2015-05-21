package org.eclipse.simrel.tests.reports;

import java.io.PrintWriter;
import java.util.Comparator;
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
      _builder.append("IU id;ReportType;Checker Class;Checker output;Checker additional data;Report creation Time");
      writer.println(_builder);
      ConcurrentLinkedQueue<CheckReport> _reports = manager.getReports();
      final Function1<CheckReport, String> _function = (CheckReport it) -> {
        IInstallableUnit _iU = it.getIU();
        return _iU.getId();
      };
      List<CheckReport> _sortBy = IterableExtensions.<CheckReport, String>sortBy(_reports, _function);
      final Comparator<CheckReport> _function_1 = (CheckReport $0, CheckReport $1) -> {
        ReportType _type = $1.getType();
        int _ordinal = _type.ordinal();
        ReportType _type_1 = $0.getType();
        int _ordinal_1 = _type_1.ordinal();
        return Integer.valueOf(_ordinal).compareTo(Integer.valueOf(_ordinal_1));
      };
      List<CheckReport> _sortWith = IterableExtensions.<CheckReport>sortWith(_sortBy, _function_1);
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
        String _additionalData = it.getAdditionalData();
        _builder_1.append(_additionalData, "");
        _builder_1.append(";");
        long _timeMs = it.getTimeMs();
        _builder_1.append(_timeMs, "");
        writer.println(_builder_1);
      };
      _sortWith.forEach(_function_2);
      writer.close();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
