package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.internal.Pair;
import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.model.Mocker.Target;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.foundation.model.HttpClientResponse;
import io.arex.inst.runtime.model.ArexConstants;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.common.util.CommonUtils;
import io.arex.standalone.local.model.DiffMocker;
import io.arex.standalone.local.util.DiffUtils;
import io.arex.standalone.local.storage.H2StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.arex.standalone.common.constant.Constants.APP_PORT;

public class ReplayHandler extends ApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayHandler.class);

    @Override
    public String process(String args) {
        Map<String, String> argMap = parseArgs(args);
        int num = Integer.parseInt(argMap.getOrDefault("num", "0"));
        String operation = argMap.get("operation");
        int port = Integer.parseInt(argMap.getOrDefault("port", APP_PORT));

        List<Pair<String, String>> idPairs = replay(num, operation, port);
        if (CollectionUtil.isEmpty(idPairs)) {
            LOGGER.warn("replay no result");
            return null;
        }
        List<DiffMocker> diffList = computeDiff(idPairs);
        if (CollectionUtil.isEmpty(diffList)) {
            return null;
        }
        return Serializer.serialize(diffList);
    }

    private List<Pair<String, String>> replay(int num, String operation, int port) {
        ArexMocker mocker = new ArexMocker(MockCategoryType.SERVLET);
        if (StringUtil.isNotEmpty(operation)) {
            mocker.setOperationName(operation);
        }
        List<Mocker> mockerList = H2StorageService.INSTANCE.queryList(mocker, num);
        if (CollectionUtil.isEmpty(mockerList)) {
            LOGGER.warn("query no result.");
            return null;
        }
        List<Pair<String, String>> pairs = new ArrayList<>();
        try {
            for (Mocker mockerInfo : mockerList) {
                HttpClientResponse response = request(mockerInfo, port);
                if (response == null || response.getHeaders() == null) {
                    continue;
                }
                pairs.add(Pair.of(mockerInfo.getRecordId(), response.getHeaders().get(ArexConstants.REPLAY_ID)));
            }
        } catch (Exception e) {
            LOGGER.warn("replay fail, please check if the port of the target application is {}. " +
                    "if not, you can specify the port through the `-p` parameter", port);
        }

        return pairs;
    }

    private List<DiffMocker> computeDiff(List<Pair<String, String>> idPairs) {
        List<DiffMocker> diffSummaryList = new ArrayList<>();
        DiffUtils dmp = new DiffUtils();
        List<Mocker> recordList;
        List<Mocker> replayList;
        String recordJson;
        String replayJson;
        for (Pair<String, String> idPair : idPairs) {
            boolean isDiff = false;
            List<DiffMocker> diffDetailList = new ArrayList<>();
            for (MockCategoryType category : MockCategoryType.values()) {
                Mocker mocker = generateMocker(category);
                mocker.setRecordId(idPair.getFirst());
                recordList = H2StorageService.INSTANCE.queryList(mocker, 0);
                mocker.setReplayId(idPair.getSecond());
                replayList = H2StorageService.INSTANCE.queryList(mocker, 0);
                if (CollectionUtil.isEmpty(recordList) && CollectionUtil.isEmpty(replayList)) {
                    continue;
                }
                int len = Math.max(recordList.size(), replayList.size());
                for (int i = 0; i < len; i++) {
                    recordJson = getCompareJson(recordList, i);
                    replayJson = getCompareJson(replayList, i);
                    if (StringUtil.isEmpty(recordJson) && StringUtil.isEmpty(replayJson)) {
                        continue;
                    }
                    Pair<String, String> diffPair = Pair.of(recordJson, replayJson);
                    String operationName = getOperationName(recordList, replayList, i);
                    diffDetailList.add(createDiffMocker(idPair, diffPair, category, operationName));
                    if (dmp.hasDiff(recordJson, replayJson)) {
                        isDiff = true;
                    }
                }
            }
            if (isDiff) {
                H2StorageService.INSTANCE.saveList(diffDetailList);
                DiffMocker diffMocker = new DiffMocker();
                diffMocker.setRecordId(idPair.getFirst());
                diffMocker.setReplayId(idPair.getSecond());
                diffMocker.setOperationName(getApi(diffDetailList));
                diffSummaryList.add(diffMocker);
            }
        }

        return diffSummaryList;
    }

    private String getOperationName(List<Mocker> recordList, List<Mocker> replayList, int index) {
        if (CollectionUtil.isNotEmpty(recordList) && recordList.size() > index) {
            return recordList.get(index).getOperationName();
        }
        if (CollectionUtil.isNotEmpty(replayList) && replayList.size() > index) {
            return replayList.get(index).getOperationName();
        }
        return "";
    }

    private String getApi(List<DiffMocker> diffList) {
        for (DiffMocker diffMocker : diffList) {
            MockCategoryType category = getCategoryType(diffMocker.getCategoryType());
            if (category != null && category.isEntryPoint()) {
                return diffMocker.getOperationName();
            }
        }
        return "";
    }

    private MockCategoryType getCategoryType(String category) {
        for (MockCategoryType categoryType : MockCategoryType.values()) {
            if (categoryType.getName().equals(category)) {
                return categoryType;
            }
        }
        return null;
    }

    private Mocker generateMocker(MockCategoryType category) {
        return new ArexMocker(category);
    }

    private String getCompareJson(List<Mocker> mockerList, int index) {
        if (mockerList.size() > index) {
            Mocker mocker = mockerList.get(index);
            if (MockCategoryType.DYNAMIC_CLASS.getName().equals(mocker.getCategoryType().getName())) {
                return "";
            }
            Target targetRequest = mocker.getTargetRequest();
            StringBuilder builder = new StringBuilder();
            builder.append("{");
            if (mocker.getCategoryType().isEntryPoint()) {
                builder.append("\"response\":").append(mocker.getTargetResponse().getBody());
            }

            switch (mocker.getCategoryType().getName()) {
                case "Database":
                    builder.append("\"parameters\":").append(targetRequest.attributeAsString("parameters")).append(",");
                    builder.append("\"sql\":\"").append(targetRequest.getBody()).append("\"");
                    break;
                case "HttpClient":
                case "DubboConsumer":
                    builder.append("\"operation\":\"").append(mocker.getOperationName()).append("\",");
                    builder.append("\"request\":\"").append(CommonUtils.decode(targetRequest.getBody())).append("\"");
                    break;
                case "Redis":
                    builder.append("\"operation\":\"").append(mocker.getOperationName()).append("\",");
                    builder.append("\"clusterName\":\"").append(targetRequest.attributeAsString("clusterName")).append("\",");
                    builder.append("\"key\":").append(targetRequest.getBody());
                    break;
                default:
                    break;
            }

            builder.append("}");
            return builder.toString();
        }
        return "";
    }

    private DiffMocker createDiffMocker(Pair<String, String> idPair, Pair<String, String> diffPair, MockCategoryType category, String operationName) {
        DiffMocker diffMocker = new DiffMocker();
        diffMocker.setRecordId(idPair.getFirst());
        diffMocker.setReplayId(idPair.getSecond());
        diffMocker.setRecordDiff(diffPair.getFirst());
        diffMocker.setReplayDiff(diffPair.getSecond());
        diffMocker.setOperationName(operationName);
        diffMocker.setCategoryType(category.getName());
        return diffMocker;
    }
}