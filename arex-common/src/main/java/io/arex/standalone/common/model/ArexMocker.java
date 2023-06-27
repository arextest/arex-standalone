package io.arex.standalone.common.model;

public class ArexMocker implements Mocker {

    private String id;
    private MockCategory categoryType;
    private String replayId;
    private String recordId;
    private String appId;
    private int recordEnvironment;
    private String recordVersion;
    private long creationTime;
    private Target targetRequest;
    private Target targetResponse;
    private String operationName;

    public ArexMocker() {
    }

    public ArexMocker(MockCategory categoryType) {
        this.categoryType = categoryType;
    }

    public String getId() {
        return this.id;
    }

    public MockCategory getCategoryType() {
        return this.categoryType;
    }

    public String getReplayId() {
        return this.replayId;
    }

    public String getRecordId() {
        return this.recordId;
    }

    public String getAppId() {
        return this.appId;
    }

    public int getRecordEnvironment() {
        return this.recordEnvironment;
    }


    public String getRecordVersion() {
        return this.recordVersion;
    }

    public void setRecordVersion(String recordVersion) {
        this.recordVersion = recordVersion;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public Target getTargetRequest() {
        return this.targetRequest;
    }

    public Target getTargetResponse() {
        return this.targetResponse;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCategoryType(MockCategory categoryType) {
        this.categoryType = categoryType;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setRecordEnvironment(int recordEnvironment) {
        this.recordEnvironment = recordEnvironment;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setTargetRequest(Target targetRequest) {
        this.targetRequest = targetRequest;
    }

    public void setTargetResponse(Target targetResponse) {
        this.targetResponse = targetResponse;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
