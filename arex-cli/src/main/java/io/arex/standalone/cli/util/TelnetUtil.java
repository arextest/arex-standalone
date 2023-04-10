package io.arex.standalone.cli.util;

import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.util.StringUtil;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.WindowSizeOptionHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class TelnetUtil {
    private static TelnetClient telnet;
    private static InputStream inputStream;
    private static PrintStream pStream;

    public static boolean connect(String ip, int tcpPort, int width, int height) {
        try {
            telnet = new TelnetClient();
            telnet.setCharset(StandardCharsets.UTF_8);
            telnet.setConnectTimeout(10000);
            TelnetOptionHandler sizeOpt = new WindowSizeOptionHandler(
                    width, height,true, true, false, false);
            telnet.addOptionHandler(sizeOpt);

            telnet.connect(ip, tcpPort);

            inputStream = telnet.getInputStream();
            OutputStream outputStream = telnet.getOutputStream();
            pStream = new PrintStream(telnet.getOutputStream());

            StringBuilder line = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int b;
            while ((b = in.read()) != -1) {
                line.appendCodePoint(b);
                if(line.toString().endsWith(Constants.CLI_PROMPT)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            close();
            LogUtil.warn(e);
        }
        return false;
    }

    public static void close() {
        if (telnet != null) {
            try {
                telnet.disconnect();
            } catch (IOException ex) {
                LogUtil.warn(ex);
            }
        }
    }

    public static void send(String command) {
        try {
            pStream.println(command);
            pStream.flush();
        } catch (Throwable e) {
            LogUtil.warn(e);
        }
    }

    public static String receive(String command) {
        try {
            StringBuilder line = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int b;
            while ((b = in.read()) != -1) {
                line.appendCodePoint(b);
                if(line.toString().endsWith(Constants.CLI_PROMPT)) {
                    break;
                }
            }
            String response = line.toString();
            if (StringUtil.isEmpty(response)) {
                return null;
            }
            StringBuilder result = new StringBuilder();
            String[] strings = response.split("\n");
            // response data
            if (strings.length > 1 && strings[0].contains(command)) {
                for (int i = 1; i < strings.length -1; i++) {
                    result.append(strings[i]);
                }
                return URLDecoder.decode(result.toString(), StandardCharsets.UTF_8.name());
            }
        } catch (Throwable e) {
            LogUtil.warn(e);
        }
        return null;
    }
}
