package com.wlx.ojbackendmodel.model.enums;

public enum DifficultyEnum {
    EASY(1, "简单", 1.0),
    MEDIUM(2, "中等", 2.0),
    HARD(3, "困难", 3.0);

    private final int value;
    private final String label;
    private final double weight;

    DifficultyEnum(int value, String label, double weight) {
        this.value = value;
        this.label = label;
        this.weight = weight;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }
    public double getWeight() { return weight; }

    public static DifficultyEnum of(int value) {
        for (DifficultyEnum e : values()) {
            if (e.value == value) return e;
        }
        return MEDIUM;
    }

    public static String getLabel(int value) {
        DifficultyEnum e = of(value);
        return e != null ? e.label : "未知";
    }
}
