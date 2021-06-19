package org.eclipse.cbi.p2repo.analyzers.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ReportWriter {
    public ReportWriter(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    private static final String EOL = System.getProperty("line.separator", "\n");
    private String              outputFilename;

    private PrintWriter         outfilewriter;

    public String getOutputFilename() {
        return outputFilename;
    }

    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public void writeln(String text) throws FileNotFoundException {
        getOutfilewriter().write(text + EOL);
    }

    public void writeln() throws FileNotFoundException {
        getOutfilewriter().write(EOL);
    }

    public void close() throws FileNotFoundException {
        if (getOutfilewriter() != null) {
            getOutfilewriter().close();
        }
    }

    public void writeln(Object object) throws FileNotFoundException {
        writeln(object.toString());
    }

    public void printf(String format, Object... args) throws FileNotFoundException {
        getOutfilewriter().printf(format, args);
    }

    private PrintWriter getOutfilewriter() throws FileNotFoundException {
        if (outfilewriter == null) {
            outfilewriter = new PrintWriter(getOutputFilename());
        }
        return outfilewriter;
    }
}
