package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.local.storage.H2StorageService;
import java.util.*;

public class LSHandler extends ApiHandler {

    @Override
    public String process(String args) throws Exception {
        Map<String, String> argMap = parseArgs(args);
        int num = Integer.parseInt(argMap.get("num"));
        String operation = argMap.get("operation");
        String recordId = argMap.get("detail");

        ArexMocker mocker = new ArexMocker(MockCategoryType.SERVLET);
        mocker.setAppId(getServiceName());

        if (StringUtil.isNotEmpty(recordId)) {
            mocker.setCategoryType(null);
            mocker.setRecordId(recordId);
            return Serializer.serialize(H2StorageService.INSTANCE.queryJsonList(mocker, num));
        }

        if (StringUtil.isNotEmpty(operation)) {
            mocker.setOperationName(operation);
            return Serializer.serialize(H2StorageService.INSTANCE.queryApiRecordId(mocker, num));
        }

        return Serializer.serialize(H2StorageService.INSTANCE.queryRecordCount(mocker, num));
    }
}