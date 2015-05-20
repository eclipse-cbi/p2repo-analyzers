/**
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * Contributors: IBM Corporation - initial API and implementation
 * This file originally came from 'Eclipse Orbit' project then adapted to use
 * in WTP and improved to use 'Manifest' to read manifest.mf, instead of reading
 * it as a properties file.
 */
package org.eclipse.simrel.tests.jars;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.eclipse.simrel.tests.RepoTestsConfiguration;
import org.eclipse.simrel.tests.common.ReportType;
import org.eclipse.simrel.tests.jars.TestJars;
import org.eclipse.simrel.tests.utils.BundleJarUtils;
import org.eclipse.simrel.tests.utils.CompositeFileFilter;
import org.eclipse.simrel.tests.utils.JARFileNameFilter;
import org.eclipse.simrel.tests.utils.PackGzFileNameFilter;
import org.eclipse.simrel.tests.utils.PlainCheckReport;
import org.eclipse.simrel.tests.utils.ReportWriter;
import org.eclipse.simrel.tests.utils.VerifyStep;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class SignerTest extends TestJars {
  public static final class SignerRunnable implements Runnable {
    private final Collection<PlainCheckReport> reports;
    
    private final File file;
    
    private final String iuTypeName;
    
    SignerRunnable(final Collection<PlainCheckReport> reports, final File file, final String iuTypeName) {
      this.reports = reports;
      this.file = file;
      this.iuTypeName = iuTypeName;
    }
    
    @Override
    public void run() {
      final PlainCheckReport checkReport = new PlainCheckReport();
      String _name = this.file.getName();
      checkReport.setFileName(_name);
      checkReport.setIuType(this.iuTypeName);
      File fileToCheck = this.file;
      String _name_1 = fileToCheck.getName();
      boolean _endsWith = _name_1.endsWith(PackGzFileNameFilter.EXTENSION_PACEKD_JAR);
      if (_endsWith) {
        try {
          File _unpack200gz = BundleJarUtils.unpack200gz(fileToCheck);
          fileToCheck = _unpack200gz;
        } catch (final Throwable _t) {
          if (_t instanceof IOException) {
            final IOException e = (IOException)_t;
            checkReport.setType(ReportType.NOT_IN_TRAIN);
            StringConcatenation _builder = new StringConcatenation();
            _builder.append("Unable to unpack ");
            String _absolutePath = this.file.getAbsolutePath();
            _builder.append(_absolutePath, "");
            _builder.append(". Can not check signature. ");
            String _message = e.getMessage();
            _builder.append(_message, "");
            checkReport.setCheckResult(_builder.toString());
            return;
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
      }
      Properties eclipseInf = BundleJarUtils.getEclipseInf(fileToCheck);
      String _property = eclipseInf.getProperty("jarprocessor.exclude.sign", "false");
      Boolean _valueOf = Boolean.valueOf(_property);
      if ((_valueOf).booleanValue()) {
        checkReport.setType(ReportType.BAD_GUY);
        checkReport.setCheckResult("Jar was excluded from signing using the eclipse.inf entry.");
      } else {
        StringBuilder errorOut = new StringBuilder();
        StringBuilder warningOut = new StringBuilder();
        final boolean verified = VerifyStep.verify(fileToCheck, errorOut, warningOut);
        if ((!verified)) {
          checkReport.setType(ReportType.NOT_IN_TRAIN);
          String _string = errorOut.toString();
          checkReport.setCheckResult(_string);
        } else {
          String message = "jar verified";
          int _length = warningOut.length();
          boolean _greaterThan = (_length > 0);
          if (_greaterThan) {
            checkReport.setType(ReportType.INFO);
            String _string_1 = warningOut.toString();
            message = _string_1;
          }
          checkReport.setCheckResult(message);
        }
      }
      this.reports.add(checkReport);
    }
  }
  
  private final static String UNSIGNED_FILENAME = "unsigned8.txt";
  
  private final static String SIGNED_FILENAME = "verified8.txt";
  
  private final static String KNOWN_UNSIGNED = "knownunsigned8.txt";
  
  public SignerTest(final RepoTestsConfiguration configurations) {
    super(configurations);
  }
  
  /**
   * @return <code>true</code> if errors were found
   */
  public boolean verifySignatures() throws IOException {
    final CopyOnWriteArraySet<PlainCheckReport> checkReports = new CopyOnWriteArraySet<PlainCheckReport>();
    boolean _canVerify = VerifyStep.canVerify();
    boolean _not = (!_canVerify);
    if (_not) {
      System.err.println("jarsigner is not available. Can not check.");
      return true;
    } else {
      String _featureDirectory = this.getFeatureDirectory();
      File _file = new File(_featureDirectory);
      this.checkJars(_file, "feature", checkReports);
      String _bundleDirectory = this.getBundleDirectory();
      File _file_1 = new File(_bundleDirectory);
      this.checkJars(_file_1, "plugin", checkReports);
    }
    final Function1<PlainCheckReport, Boolean> _function = new Function1<PlainCheckReport, Boolean>() {
      @Override
      public Boolean apply(final PlainCheckReport it) {
        ReportType _type = it.getType();
        return Boolean.valueOf(Objects.equal(_type, ReportType.NOT_IN_TRAIN));
      }
    };
    final boolean containsErrors = IterableExtensions.<PlainCheckReport>exists(checkReports, _function);
    this.printSummary(checkReports);
    return containsErrors;
  }
  
  public void checkJars(final File dirToCheck, final String iuType, final CopyOnWriteArraySet<PlainCheckReport> reports) {
    JARFileNameFilter _jARFileNameFilter = new JARFileNameFilter();
    PackGzFileNameFilter _packGzFileNameFilter = new PackGzFileNameFilter();
    CompositeFileFilter _create = CompositeFileFilter.create(_jARFileNameFilter, _packGzFileNameFilter);
    final File[] jars = dirToCheck.listFiles(_create);
    int nFiles = jars.length;
    int TIME_PER_FILE = 2;
    int TOTAL_WAIT_SECONDS = (nFiles * TIME_PER_FILE);
    ExecutorService threadPool = Executors.newFixedThreadPool(64);
    for (final File file : jars) {
      SignerTest.SignerRunnable _signerRunnable = new SignerTest.SignerRunnable(reports, file, iuType);
      threadPool.execute(_signerRunnable);
    }
    threadPool.shutdown();
    try {
      boolean _awaitTermination = threadPool.awaitTermination(TOTAL_WAIT_SECONDS, TimeUnit.SECONDS);
      boolean _not = (!_awaitTermination);
      if (_not) {
        threadPool.shutdownNow();
        boolean _awaitTermination_1 = threadPool.awaitTermination(5, TimeUnit.MINUTES);
        boolean _not_1 = (!_awaitTermination_1);
        if (_not_1) {
          System.err.println("ThreadPool did not terminate within time limits.");
        }
      }
    } catch (final Throwable _t) {
      if (_t instanceof InterruptedException) {
        final InterruptedException ie = (InterruptedException)_t;
        threadPool.shutdownNow();
        Thread _currentThread = Thread.currentThread();
        _currentThread.interrupt();
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  private void printSummary(final Set<PlainCheckReport> reports) throws IOException {
    ReportWriter info = this.createNewReportWriter(SignerTest.SIGNED_FILENAME);
    ReportWriter warn = this.createNewReportWriter(SignerTest.KNOWN_UNSIGNED);
    ReportWriter error = this.createNewReportWriter(SignerTest.UNSIGNED_FILENAME);
    try {
      final Function1<PlainCheckReport, Integer> _function = new Function1<PlainCheckReport, Integer>() {
        @Override
        public Integer apply(final PlainCheckReport it) {
          String _fileName = it.getFileName();
          return Integer.valueOf(_fileName.length());
        }
      };
      List<PlainCheckReport> _sortBy = IterableExtensions.<PlainCheckReport, Integer>sortBy(reports, _function);
      PlainCheckReport _last = IterableExtensions.<PlainCheckReport>last(_sortBy);
      String _fileName = _last.getFileName();
      final int longestFileName = _fileName.length();
      final Function1<PlainCheckReport, String> _function_1 = new Function1<PlainCheckReport, String>() {
        @Override
        public String apply(final PlainCheckReport it) {
          return it.getFileName();
        }
      };
      List<PlainCheckReport> _sortBy_1 = IterableExtensions.<PlainCheckReport, String>sortBy(reports, _function_1);
      for (final PlainCheckReport report : _sortBy_1) {
        {
          String _fileName_1 = report.getFileName();
          int _length = _fileName_1.length();
          int _minus = (longestFileName - _length);
          final String indent = Strings.repeat(" ", _minus);
          StringConcatenation _builder = new StringConcatenation();
          String _fileName_2 = report.getFileName();
          _builder.append(_fileName_2, "");
          _builder.append(indent, "");
          _builder.append(" ");
          String _iuType = report.getIuType();
          _builder.append(_iuType, "");
          _builder.append("\t\t");
          String _checkResult = report.getCheckResult();
          _builder.append(_checkResult, "");
          final String line = _builder.toString();
          ReportType _type = report.getType();
          if (_type != null) {
            switch (_type) {
              case INFO:
                info.writeln(line);
                break;
              case NOT_IN_TRAIN:
                error.writeln(line);
                break;
              case BAD_GUY:
                warn.writeln(line);
                break;
              default:
                break;
            }
          }
        }
      }
    } finally {
      info.close();
      warn.close();
      error.close();
    }
  }
  
  protected ReportWriter createNewReportWriter(final String filename) {
    StringConcatenation _builder = new StringConcatenation();
    String _reportOutputDirectory = this.getReportOutputDirectory();
    _builder.append(_reportOutputDirectory, "");
    _builder.append("/");
    _builder.append(filename, "");
    return new ReportWriter(_builder.toString());
  }
}
