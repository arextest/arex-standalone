package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.standalone.common.util.StringUtil;
import io.arex.standalone.local.model.DiffMocker;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.local.storage.H2StorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WatchHandler extends ApiHandler {

    @Override
    public String process(String args) throws Exception {
        Map<String, String> argMap = parseArgs(args);
        int num = Integer.parseInt(argMap.getOrDefault("num", "0"));
        String replayId = argMap.get("replayId");

        DiffMocker mocker = new DiffMocker();
        if (StringUtil.isNotEmpty(replayId)) {
            mocker.setReplayId(replayId);
            num = 0;
        } else {
            mocker.setCategoryType(MockCategoryType.SERVLET.getName());
        }
        List<DiffMocker> diffList = H2StorageService.INSTANCE.queryList(mocker, num);
        if (CollectionUtil.isEmpty(diffList)) {
            return null;
        }
        return Serializer.serialize(diffList);
    }
}