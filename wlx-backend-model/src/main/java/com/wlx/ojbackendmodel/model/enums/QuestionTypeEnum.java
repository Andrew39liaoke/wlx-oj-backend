package com.wlx.ojbackendmodel.model.enums;

public enum QuestionTypeEnum {
    SINGLE(1, "单选题"),
    MULTIPLE(2, "多选题");

    private final int value;
    private final String label;

    QuestionTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static QuestionTypeEnum of(int value) {
        for (QuestionTypeEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
