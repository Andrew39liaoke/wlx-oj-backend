package com.wlx.ojbackendmodel.model.enums;

public enum RecommendTypeEnum {
    RULE(1, "规则推荐"),
    GRAPH(2, "知识图谱推荐"),
    SIMILARITY(3, "相似度推荐");

    private final int value;
    private final String label;

    RecommendTypeEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }
}
