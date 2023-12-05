package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.foundation.model.HttpClientResponse;
import io.arex.standalone.local.storage.H2StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.arex.standalone.common.constant.Constants.APP_PORT;

public class DebugHandler extends ApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugHandler.class);
    @Override
    public String process(String args) throws Exception {
        Map<String, String> argMap = parseArgs(args);
        String recordId = argMap.get("recordId");
        int port = Integer.parseInt(argMap.getOrDefault("port", APP_PORT));

        Mocker mocker = new ArexMocker(MockCategoryType.SERVLET);
        mocker.setRecordId(recordId);
        Mocker resultMocker = H2StorageService.INSTANCE.query(mocker);
        if (resultMocker == null) {
            return "query no result.";
        }
        HttpClientResponse response = null;
        try {
            response = request(resultMocker, port);
        } catch (Exception e) {
            LOGGER.warn("debug fail, please check if the port of the target application is {}. " +
                    "if not, you can specify the port through the `-p` parameter", port);
        }
        if (response == null) {
            return "response is null.";
        }
        return response.getBody();
    }
}