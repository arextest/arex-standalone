package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.model.Mocker.Target;
import io.arex.foundation.util.AsyncHttpClientUtil;
import io.arex.standalone.common.util.CommonUtils;
import io.arex.standalone.common.constant.Constants;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiHandler {

    public Map<String, String> request(Mocker servletMocker, int port) {
        if (!servletMocker.getCategoryType().isEntryPoint()) {
            return Collections.emptyMap();
        }
        Target target = servletMocker.getTargetRequest();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/json;charset=UTF-8");
        requestHeaders.put("arex-record-id", servletMocker.getRecordId());

        String request = StringUtils.isNotBlank(target.getBody()) ? CommonUtils.decode(target.getBody()) : "";
        String url = "http://localhost:" + port + target.attributeAsString("RequestPath");
        return AsyncHttpClientUtil.executeAsyncIncludeHeader(url, request, requestHeaders).join();
    }

    protected Map<String, String> parseArgs(String argument) {
        if (StringUtils.isBlank(argument)) {
            return null;
        }
        String[] args = StringUtils.splitByWholeSeparator(argument.trim(), Constants.CLI_SEPARATOR);
        Map<String, String> argMap = new LinkedHashMap<>();
        for (String arg : args) {
            if (StringUtils.isBlank(arg)) {
                continue;
            }
            String[] options = parseOption(arg);
            if (options.length > 1) {
                argMap.put(options[0], options[1]);
            } else {
                argMap.put(options[0], StringUtils.EMPTY);
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