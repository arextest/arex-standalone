package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.standalone.common.DiffMocker;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.local.storage.H2StorageService;

import java.util.ArrayList;
import java.util.List;

public class WatchHandler extends ApiHandler {

    @Override
    public String process(String args) throws Exception {
        List<DiffMocker> diffList = new ArrayList<>();
        DiffMocker mocker = new DiffMocker();
        for (MockCategoryType category : MockCategoryType.values()) {
            mocker.setReplayId(args);
            mocker.setCategoryType(category);
            List<DiffMocker> diffMockers = H2StorageService.INSTANCE.queryList(mocker);
            if (CollectionUtil.isNotEmpty(diffMockers)) {
                diffList.addAll(diffMockers);
            }
        }
        if (CollectionUtil.isEmpty(diffList)) {
            return null;
        }
        return Serializer.serialize(diffList);
    }
}