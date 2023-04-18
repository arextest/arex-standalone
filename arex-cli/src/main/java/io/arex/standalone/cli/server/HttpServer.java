package io.arex.standalone.cli.server;

import io.arex.standalone.cli.cmd.RootCommand;
import io.arex.standalone.cli.server.process.AbstractProcessor;
import io.arex.standalone.cli.server.process.DebugProcessor;
import io.arex.standalone.cli.server.process.DetailProcessor;
import io.arex.standalone.cli.server.process.ListProcessor;
import io.arex.standalone.cli.util.LogUtil;
import io.arex.standalone.common.serializer.Serializer;
import io.arex.standalone.common.util.*;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static io.arex.standalone.common.constant.Constants.WRAP;

public class HttpServer {
    private static final String ROOT = "/";
    private static Map<String, AbstractProcessor> processorMap = new HashMap<>();

    static {
        init();
    }

    public static void handle(Socket socket) throws IOException {
        InputStreamReader inputStream = new InputStreamReader(socket.getInputStream());
        StringBuilder request = new StringBuilder();
        char[] charBuf = new char[1024];
        int mark;
        while ((mark = inputStream.read(charBuf)) != -1) {
            request.append(charBuf, 0, mark);
            if (mark < charBuf.length) {
                break;
            }
        }
        String[] requests = request.toString().split(WRAP);
        try (OutputStream outputStream = socket.getOutputStream()) {
            String path = getPath(requests);
            outputStream.write(buildResponseHeader(path).getBytes(StandardCharsets.UTF_8));
            if (!valid(path, outputStream)) {
                return;
            }
            String response = process(path, requests[requests.length - 1]);
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean valid(String path, OutputStream outputStream) throws IOException {
        if (indexOrAssets(path)) {
            return true;
        }
        if (path.contains("favicon.ico")) {
            return false;
        }
        if (!processorMap.containsKey(path)) {
            outputStream.write(error("invalid uri"));
            return false;
        }
        if (StringUtil.isEmpty(RootCommand.currentCmd())) {
            outputStream.write(error("please execute the command first"));
            return false;
        }
        return true;
    }

    private static String buildResponseHeader(String path) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK").append(WRAP);
        if (StringUtil.isEmpty(path)) { // home page
            response.append("Content-Type: text/html; charset=utf-8").append(WRAP);
        } else if (path.endsWith(".js")) {
            response.append("Content-Type: application/javascript; charset=utf-8").append(WRAP);
        } else if (path.endsWith(".css")) {
            response.append("Content-Type: text/css; charset=utf-8").append(WRAP);
        } else if (path.endsWith(".svg")) {
            response.append("Content-Type: image/svg+xml; charset=utf-8").append(WRAP);
        }else {
            response.append("Content-Type: application/json; charset=utf-8").append(WRAP);
        }
        response.append("Access-Control-Allow-Origin: *").append(WRAP);
        response.append("Connection: keep-alive").append(WRAP);
        response.append("Accept: */*").append(WRAP);
        response.append(WRAP);
        return response.toString();
    }

    private static String getPath(String[] requests) {
        String path = StringUtils.substringBetween(requests[0], " ", " ");
        path = path == null ? "" : path;
        if (path.startsWith(ROOT)) {
            return path.substring(1).toLowerCase();
        }
        return path;
    }

    private static String process(String path, String param) {
        try {
            if (indexOrAssets(path)) {
                return homePage(path);
            }
            Request request = parseParam(param);
            return processorMap.get(path).process(request);
        } catch (Exception e) {
            LogUtil.warn(e);
            return StringUtil.format("server process error:{}", e.toString());
        }
    }

    private static boolean indexOrAssets(String path) {
        return StringUtil.isEmpty(path) || path.startsWith("assets/");
    }

    private static Request parseParam(String param) {
        if (param != null && param.contains("{") && param.contains("}")) {
            return Serializer.deserialize(param, Request.class);
        }
        return null;
    }

    private static String homePage(String path) {
        String result = "";
        InputStream inputStream = null;
        try {
            inputStream = HttpServer.class.getResourceAsStream("/static/" + ("".equals(path) ? "index.html" : path));
            result = IOUtils.toString(inputStream);
        } catch (Throwable e) {
            LogUtil.warn(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
        }
        return result;
    }

    private static void init(){
        register(new ListProcessor());
        register(new DetailProcessor());
        register(new DebugProcessor());
    }

    private static void register(AbstractProcessor processor){
        String key = processor.getClass().getSimpleName();
        processorMap.put(key.substring(0, key.indexOf("Processor")).toLowerCase(), processor);
    }

    private static byte[] error(String msg) {
        return StringUtil.format("{\"errorMessage\":\"{}\"}", msg).getBytes(StandardCharsets.UTF_8);
    }
}
