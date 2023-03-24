package io.arex.standalone.common.model;

public class RecordModel {
    private String recordId;
    private String operationName;
    private String caseNum;
    private String mockCategoryType;
    private String message;
    private String index;
    public RecordModel() {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}