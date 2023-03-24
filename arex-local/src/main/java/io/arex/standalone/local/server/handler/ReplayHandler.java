package io.arex.standalone.local.server.handler;

import io.arex.agent.bootstrap.internal.Pair;
import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.model.Mocker.Target;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.common.CommonUtils;
import io.arex.standalone.common.DiffMocker;
import io.arex.standalone.local.util.DiffUtils;
import io.arex.standalone.local.util.JsonUtil;
import io.arex.standalone.local.storage.H2StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReplayHandler extends ApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayHandler.class);

    @Override
    public String process(String args) {
        Map<String, String> argMap = parseArgs(args);
        int num = Integer.parseInt(argMap.get("num"));
        String operation = argMap.get("operation");

        List<Pair<String, String>> idPairs = replay(num, operation);
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

    private List<Pair<String, String>> replay(int num, String operation) {
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
        for (Mocker mockerInfo : mockerList) {
            Map<String, String> responseMap = request(mockerInfo);
            pairs.add(Pair.of(mockerInfo.getRecordId(), responseMap.get("arex-replay-id")));
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
            boolean hasDiff = false;
            int diffCount = 0;
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
                String operationName = getOperationName(recordList, replayList);
                for (int i = 0; i < len; i++) {
                    recordJson = getCompareJson(recordList, i, category);
                    replayJson = getCompareJson(replayList, i, category);
                    if (recordJson == null && replayJson == null) {
                        continue;
                    }
                    Pair<String, String> dbDiffPair = dmp.diff(JsonUtil.formatJson(recordJson), JsonUtil.formatJson(replayJson));
                    diffCount += dmp.diffCount(dbDiffPair);
                    diffDetailList.add(getDiffMocker(idPair, dbDiffPair, category, operationName));
                }
                if (diffCount > 0) {
                    hasDiff = true;
                }
            }
            if (hasDiff) {
                H2StorageService.INSTANCE.saveList(diffDetailList);
                DiffMocker diffMocker = new DiffMocker();
                diffMocker.setRecordId(idPair.getFirst());
                diffMocker.setReplayId(idPair.getSecond());
                diffMocker.setDiffCount(diffCount);
                diffSummaryList.add(diffMocker);
            }
        }

        return diffSummaryList;
    }

    private String getOperationName(List<Mocker> recordList, List<Mocker> replayList) {
        if (CollectionUtil.isNotEmpty(recordList)) {
            return recordList.get(0).getOperationName();
        }
        if (CollectionUtil.isNotEmpty(replayList)) {
            return replayList.get(0).getOperationName();
        }
        return "";
    }

    private Mocker generateMocker(MockCategoryType category) {
        return new ArexMocker(category);
    }

    private String getCompareJson(List<Mocker> mockerList, int index, MockCategoryType category) {
        Map<String, String> compareMap = new HashMap<>();
        if (mockerList.size() > index) {
            Mocker mocker = mockerList.get(index);
            Target targetRequest = mocker.getTargetRequest();
            if (category.isEntryPoint()) {
                compareMap.put("response", JsonUtil.formatJson(mocker.getTargetResponse().getBody()));
            }
            if (category == MockCategoryType.DATABASE) {
                compareMap.put("dbname", targetRequest.attributeAsString("dbName"));
                compareMap.put("parameters", targetRequest.attributeAsString("parameters"));
                compareMap.put("sql", targetRequest.getBody());
            }
            if (category == MockCategoryType.HTTP_CLIENT) {
                compareMap.put("operation", mocker.getOperationName());
                compareMap.put("request", CommonUtils.decode(targetRequest.getBody()));
            }
            if (category == MockCategoryType.REDIS) {
                compareMap.put("clusterName", targetRequest.attributeAsString("clusterName"));
                compareMap.put("key", targetRequest.getBody());
            }
        }

        if (compareMap.size() > 1) {
            return Serializer.serialize(compareMap);
        }
        return compareMap.values().stream().filter(Objects::nonNull).findFirst().orElse(null);
    }

    private DiffMocker getDiffMocker(Pair<String, String> idPair, Pair<String, String> diffPair, MockCategoryType category, String operationName) {
        DiffMocker diffMocker = new DiffMocker(category);
        diffMocker.setRecordId(idPair.getFirst());
        diffMocker.setReplayId(idPair.getSecond());
        diffMocker.setRecordDiff(diffPair.getFirst());
        diffMocker.setReplayDiff(diffPair.getSecond());
        diffMocker.setOperationName(operationName);
        return diffMocker;
    }
}