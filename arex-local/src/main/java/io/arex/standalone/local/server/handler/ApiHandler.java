package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.model.Mocker.Target;
import io.arex.foundation.util.AsyncHttpClientUtil;
import io.arex.standalone.common.CommonUtils;
import io.arex.standalone.common.Constants;
import org.apache.commons.lang3.StringUtils;
import shaded.apache.http.HttpEntity;
import shaded.apache.http.HttpHeaders;
import shaded.apache.http.entity.ByteArrayEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class ApiHandler {

    public Map<String, String> request(Mocker servletMocker) {
        if (!servletMocker.getCategoryType().isEntryPoint()) {
            return Collections.emptyMap();
        }
        Target target = servletMocker.getTargetRequest();

        Map<String, String> mockerHeader = (Map<String, String>) target.getAttribute("Headers");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        requestHeaders.put("arex-record-id", servletMocker.getRecordId());

        String request = StringUtils.isNotBlank(target.getBody()) ? CommonUtils.decode(target.getBody()) : "";
        HttpEntity httpEntity = new ByteArrayEntity(request.getBytes(StandardCharsets.UTF_8));
        String url = "http://" + mockerHeader.get("host") + target.attributeAsString("RequestPath");
        return AsyncHttpClientUtil.executeAsyncIncludeHeader(url, httpEntity, requestHeaders).join();
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