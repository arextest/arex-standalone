package io.arex.standalone.cli.util;

import io.arex.standalone.common.util.StringUtil;
import java.io.*;
import java.time.LocalDateTime;

public class LogUtil {

    private static PrintStream ps = System.out;

    private static String arexLogDir = "";

    static {
        try {
            String arexDir = File.separator + ".arex" + File.separator + "log" + File.separator;
            File logDir = new File(System.getProperty("user.home") + arexDir);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            if (!logDir.exists()) {
                logDir = new File(System.getProperty("java.io.tmpdir") + arexDir);
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
            }

            File log = new File(logDir, "arex.log");

            if (!log.exists()) {
                log.createNewFile();
            }
            arexLogDir = log.getAbsolutePath();
            ps = new PrintStream(new FileOutputStream(log, true));
        } catch (Throwable t) {
            t.printStackTrace(ps);
        }
    }

    public static void info(String from, Object... arguments) {
        String title = LocalDateTime.now() + " [INFO] ";
        ps.println(title + format(from, arguments));
        ps.flush();
    }

    public static void warn(String from, Object... arguments) {
        String title = LocalDateTime.now() + " [WARN] ";
        ps.println(title + format(from, arguments));
        ps.flush();
    }

    public static void warn(Throwable ex) {
        String title = LocalDateTime.now() + " [WARN] ";
        ps.println(title + getStackTrace(ex));
        ps.flush();
    }

    public static String format(String from, Object... arguments) {
        return StringUtil.format(from, arguments);
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static String getLogDir() {
        return arexLogDir;
    }
}
