package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.model.Mocker.Target;
import io.arex.foundation.model.HttpClientResponse;
import io.arex.foundation.util.httpclient.AsyncHttpClientUtil;
import io.arex.standalone.common.util.CommonUtils;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.util.StringUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiHandler {

    public HttpClientResponse request(Mocker servletMocker, int port) {
        if (!servletMocker.getCategoryType().isEntryPoint()) {
            return null;
        }
        Target target = servletMocker.getTargetRequest();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json;charset=UTF-8");
        requestHeaders.put("arex-record-id", servletMocker.getRecordId());

        String request = StringUtil.isNotEmpty(target.getBody()) ? CommonUtils.decode(target.getBody()) : "";
        String url = "http://localhost:" + port + target.attributeAsString("RequestPath");
        return AsyncHttpClientUtil.postAsyncWithJson(url, request, requestHeaders).join();
    }

    protected Map<String, String> parseArgs(String argument) {
        if (StringUtil.isBlank(argument)) {
            return null;
        }
        String[] args = StringUtil.splitByWholeSeparator(argument.trim(), Constants.CLI_SEPARATOR);
        Map<String, String> argMap = new LinkedHashMap<>();
        for (String arg : args) {
            if (StringUtil.isBlank(arg)) {
                continue;
            }
            String[] options = parseOption(arg);
            if (options.length > 1) {
                argMap.put(options[0], options[1]);
            } else {
                argMap.put(options[0], StringUtil.EMPTY);
            }
        }
        return argMap;
    }

    private String[] parseOption(String args) {
        String str = args.trim();
        int index = str.indexOf("=", 0);
        String key = str.substring(0, index);
        String val = str.substring(index+1);
        return new String[]{key, val};
    }

    public abstract String process(String args) throws Exception;

    protected String getServiceName() {
        return System.getProperty("arex.service.name");
    }
}