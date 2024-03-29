package io.arex.standalone.local.server.handler;

import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.util.StringUtil;
import io.termd.core.readline.Function;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ServerHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

    private static Map<String, ApiHandler> handlerMap;

    public static void handle(TtyConnection conn) {
        if (handlerMap == null) {
            init();
        }

        readline(new Readline(Keymap.getDefault()).addFunctions(Function.loadDefaults()), conn);
    }

    public static void readline(Readline readline, TtyConnection conn) {
        readline.readline(conn, Constants.CLI_PROMPT, line -> {
            line = StringUtil.defaultString(line);
            if (StringUtil.equalsIgnoreCase(line, "exit")) {
                conn.write("exit\n").close();
            } else {
                String response;
                String command = "";
                try {
                    String[] lines = line.split(" ");
                    String args = "";
                    command = lines[0];
                    if (lines.length > 1) {
                        args = lines[1];
                    }
                    ApiHandler apiHandler = handlerMap.get(command);
                    if (apiHandler == null) {
                        response = "invalid command";
                    } else {
                        response = apiHandler.process(args);
                    }
                    response = response == null ? "" : URLEncoder.encode(response, StandardCharsets.UTF_8.name());
                } catch (Throwable e) {
                    LOGGER.warn("execute command: " + command + " fail", e);
                    response = e.getMessage();
                }
                conn.write(response + "\n");
                // Read line again
                readline(readline, conn);
            }
        });
    }

    private static void init(){
        handlerMap = new HashMap<>();
        register(new ReplayHandler());
        register(new WatchHandler());
        register(new DebugHandler());
        register(new MetricHandler());
        register(new LSHandler());
    }

    private static void register(ApiHandler handler){
        String key = handler.getClass().getSimpleName();
        handlerMap.put(key.substring(0, key.indexOf("Handler")).toLowerCase(), handler);
    }
}
