package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.standalone.local.storage.H2StorageService;

import java.util.Map;

public class DebugHandler extends ApiHandler {
    @Override
    public String process(String args) throws Exception {
        Map<String, String> argMap = parseArgs(args);
        String recordId = argMap.get("recordId");
        int port = Integer.parseInt(argMap.get("port"));

        Mocker mocker = new ArexMocker(MockCategoryType.SERVLET);
        mocker.setRecordId(recordId);
        Mocker resultMocker = H2StorageService.INSTANCE.query(mocker);
        if (resultMocker == null) {
            return "query no result.";
        }
        Map<String, String> responseMap = request(resultMocker, port);
        if (responseMap == null) {
            return "response is null.";
        }
        return responseMap.get("responseBody");
    }
}