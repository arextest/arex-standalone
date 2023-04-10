package io.arex.standalone.common.model;

public class LocalModel {
    private String recordId;
    private String replayId;
    private String operationName;
    private String caseNum;
    private String mockCategoryType;
    private String recordJson;
    private String replayJson;
    private String index;
    public LocalModel() {
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public String getCaseNum() {
        return caseNum;
    }

    public void setCaseNum(String caseNum) {
        this.caseNum = caseNum;
    }

    public String getMockCategoryType() {
        return mockCategoryType;
    }

    public void setMockCategoryType(String mockCategoryType) {
        this.mockCategoryType = mockCategoryType;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public String getRecordJson() {
        return recordJson;
    }

    public void setRecordJson(String recordJson) {
        this.recordJson = recordJson;
    }

    public String getReplayJson() {
        return replayJson;
    }

    public void setReplayJson(String replayJson) {
        this.replayJson = replayJson;
    }
}