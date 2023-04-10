package io.arex.standalone.cli.server.process;

import io.arex.standalone.cli.cmd.RootCommand;
import io.arex.standalone.cli.server.Request;
import io.arex.standalone.cli.util.TelnetUtil;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.model.*;
import io.arex.standalone.common.serializer.Serializer;
import io.arex.standalone.common.util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DetailProcessor extends AbstractProcessor {
    @Override
    public String process(Request request) throws Exception {
        if ("ls".equals(RootCommand.currentCmd())) {
            return recordDetail(request);
        }
        return watchDetail(request);
    }

    private String recordDetail(Request request) {
        String recordId = getRecordId(request);
        if (StringUtil.isEmpty(recordId)) {
            return fail("recordId is empty");
        }
        StringBuilder options = new StringBuilder(" ");
        options.append("detail=").append(recordId).append(Constants.CLI_SEPARATOR);
        TelnetUtil.send("ls" + options);
        String response = TelnetUtil.receive("ls");
        List<LocalModel> resultList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_LOCAL));
        if (CollectionUtil.isEmpty(resultList)) {
            return fail("query detail is empty");
        }
        for (LocalModel localModel : resultList) {
            Mocker mocker = Serializer.deserialize(localModel.getRecordJson(), ArexMocker.class);
            if (mocker == null) {
                continue;
            }
            String json = CommonUtils.generateMockerJson(mocker);
            if (StringUtil.isEmpty(json)) {
                continue;
            }
            localModel.setRecordJson(json);
        }
        return Serializer.serialize(resultList);
    }

    private String watchDetail(Request request) {
        String replayId = getReplayId(request);
        if (StringUtil.isEmpty(replayId)) {
            return fail("replayId is empty");
        }
        StringBuilder options = new StringBuilder(" ");
        options.append("replayId=").append(replayId).append(Constants.CLI_SEPARATOR);
        TelnetUtil.send("watch" + options);
        String response = TelnetUtil.receive("watch");
        if (StringUtil.isEmpty(response) || !response.contains("{")) {
            return fail("query detail is empty");
        }
        List<DiffModel> diffList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_DIFF));
        if (CollectionUtil.isEmpty(diffList)) {
            return fail("deserialize result is empty");
        }

        sortOperation(diffList);
        List<LocalModel> replayList = new ArrayList<>();
        for (DiffModel diffModel : diffList) {
            LocalModel localModel = new LocalModel();
            localModel.setRecordId(diffModel.getRecordId());
            localModel.setReplayId(diffModel.getReplayId());
            localModel.setOperationName(diffModel.getOperationName());
            localModel.setMockCategoryType(diffModel.getCategoryType());
            localModel.setRecordJson(diffModel.getRecordDiff());
            localModel.setReplayJson(diffModel.getReplayDiff());
            replayList.add(localModel);
        }

        return Serializer.serialize(replayList);
    }

    private void sortOperation(List<DiffModel> diffList) {
        DiffModel mainMocker = null;
        for (DiffModel diffMocker : diffList) {
            MockCategory category = MockCategory.getByName(diffMocker.getCategoryType());
            if (category.isEntryPoint()) {
                mainMocker = diffMocker;
                break;
            }
        }
        if (mainMocker != null) {
            diffList.remove(mainMocker);
            diffList.add(0, mainMocker);
        }
    }
}
