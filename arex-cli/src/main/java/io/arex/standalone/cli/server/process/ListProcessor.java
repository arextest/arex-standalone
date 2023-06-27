package io.arex.standalone.cli.server.process;

import io.arex.inst.runtime.util.TypeUtil;
import io.arex.standalone.cli.cmd.RootCommand;
import io.arex.standalone.cli.server.Request;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.model.DiffModel;
import io.arex.standalone.common.model.LocalModel;
import io.arex.standalone.common.util.CollectionUtil;
import io.arex.standalone.common.util.SerializeUtils;
import io.arex.standalone.common.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ListProcessor extends AbstractProcessor {
    @Override
    public String process(Request request) throws Exception {
        String response = RootCommand.query(RootCommand.currentCmd());
        if (StringUtil.isEmpty(response) || !response.contains("{")) {
            return fail("query result is empty, please confirm if the command line has found any results");
        }

        if ("ls".equals(RootCommand.currentCmd())) {
            if (StringUtil.isEmpty(RootCommand.currentApi())) {
                return fail("operation name is empty, please execute the command: ls -o first");
            }
            return response;
        }

        return parse(response);
    }

    private String parse(String response) {
        List<DiffModel> diffList = SerializeUtils.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_DIFF));
        if (CollectionUtil.isEmpty(diffList)) {
            return fail("deserialize result is empty");
        }

        List<LocalModel> replayList = new ArrayList<>();
        for (DiffModel diffModel : diffList) {
            LocalModel localModel = new LocalModel();
            localModel.setRecordId(diffModel.getRecordId());
            localModel.setReplayId(diffModel.getReplayId());
            localModel.setOperationName(diffModel.getOperationName());
            localModel.setMockCategoryType(diffModel.getCategoryType());
            replayList.add(localModel);
        }
        return SerializeUtils.serialize(replayList);
    }
}
