package com.wlx.ojbackendmodel.model.enums;

public enum ExamRecordStatusEnum {
    ANSWERING(0, "答题中"),
    SUBMITTED(1, "已提交"),
    GRADED(2, "已批改");

    private final int value;
    private final String label;

    ExamRecordStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }
}
