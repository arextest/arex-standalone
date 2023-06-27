package io.arex.standalone.cli.server.process;

import io.arex.standalone.cli.server.Request;
import io.arex.standalone.common.util.StringUtil;

public abstract class AbstractProcessor {
    public abstract String process(Request request) throws Exception;
    public String getRecordId(Request request) {
        return request != null ? request.getRecordId() : "";
    }
    public String getReplayId(Request request) {
        return request != null ? request.getReplayId() : "";
    }
    public String fail(String message) {
        return StringUtil.format("{\"errorMessage\":\"{}\"}", message);
    }
}
