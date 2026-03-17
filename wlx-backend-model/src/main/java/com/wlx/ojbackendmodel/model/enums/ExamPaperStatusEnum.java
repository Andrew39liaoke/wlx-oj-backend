package com.wlx.ojbackendmodel.model.enums;

public enum ExamPaperStatusEnum {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    FINISHED(2, "已结束");

    private final int value;
    private final String label;

    ExamPaperStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static String getLabel(int value) {
        for (ExamPaperStatusEnum e : values()) {
            if (e.value == value) return e.label;
        }
        return "未知";
    }
}
