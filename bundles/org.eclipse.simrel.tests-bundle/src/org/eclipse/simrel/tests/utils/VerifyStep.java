package org.eclipse.simrel.tests.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Properties;

import org.eclipse.equinox.internal.p2.jarprocessor.StreamProcessor;
import org.eclipse.equinox.internal.p2.jarprocessor.Utils;

public class VerifyStep {

    private static final String JAR_VERIFIED  = "jar verified.";
    static String               verifyCommand = "jarsigner";    //$NON-NLS-1$
    static Boolean              canVerify     = null;

    public static boolean canVerify() {
        if (canVerify != null)
            return canVerify.booleanValue();

        String javaHome = System.getProperty("java.home"); //$NON-NLS-1$
        String command = javaHome + "/../bin/jarsigner"; //$NON-NLS-1$
        int result = execute(new String[] { command }, false);
        if (result < 0) {
            command = "jarsigner"; //$NON-NLS-1$
            result = execute(new String[] { command }, false);
            if (result < 0) {
                canVerify = Boolean.FALSE;
                return false;
            }
        }
        verifyCommand = command;
        canVerify = Boolean.TRUE;
        return true;
    }

    public String getStepName() {
        return "Verify"; //$NON-NLS-1$
    }

    public File postProcess(File input, File workingDirectory, List<Properties> containers) {
        return null;
    }

    public static boolean verify(File input, StringBuilder errors, StringBuilder warnings) {
        if (canVerify() && verifyCommand != null) {
            try {
                String[] cmd = new String[] { verifyCommand, "-verify", input.getCanonicalPath() }; //$NON-NLS-1$
                ProcessBuilder procBuilder = new ProcessBuilder(cmd);
                procBuilder.redirectErrorStream(true);

                Process resultProc = procBuilder.start();
                int result = resultProc.waitFor();
                if (result != 0) {
                    errors.append("Error: " + result + " was returned from command: " + Utils.concat(cmd)); //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(resultProc.getInputStream()));
                StringBuilder out = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                }
                reader.close();

                String outString = out.toString();
                if (!JAR_VERIFIED.equals(outString)) {
                    if (outString.contains("Warning:")) {
                        warnings.append(outString);
                    } else {
                        errors.append(out);
                        return false;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
        return false;
    }

    protected static int execute(String[] cmd, boolean verbose) {
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        try {
            proc = runtime.exec(cmd);
            StreamProcessor.start(proc.getInputStream(), StreamProcessor.STDOUT, verbose);
        } catch (Exception e) {
            if (verbose) {
                System.out.println("Error executing command " + Utils.concat(cmd)); //$NON-NLS-1$
                e.printStackTrace();
            }
            return -1;
        }
        try {
            int result = proc.waitFor();
            return result;
        } catch (InterruptedException e) {
            if (verbose)
                e.printStackTrace();
        }
        return -1;
    }
}