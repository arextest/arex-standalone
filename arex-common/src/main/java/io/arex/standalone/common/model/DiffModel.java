package io.arex.standalone.common.model;

public class DiffModel {
    private String recordId;
    private String replayId;
    private String recordDiff;
    private String replayDiff;
    private String categoryType;
    private int diffCount;
    private String operationName;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public String getRecordDiff() {
        return recordDiff;
    }

    public void setRecordDiff(String recordDiff) {
        this.recordDiff = recordDiff;
    }

    public String getReplayDiff() {
        return replayDiff;
    }

    public void setReplayDiff(String replayDiff) {
        this.replayDiff = replayDiff;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public int getDiffCount() {
        return diffCount;
    }

    public void setDiffCount(int diffCount) {
        this.diffCount = diffCount;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}