package com.wlx.ojbackendmodel.model.enums;

public enum MasteryLevelEnum {
    WEAK(1, "薄弱"),
    FAIR(2, "一般"),
    GOOD(3, "掌握良好");

    private final int value;
    private final String label;

    MasteryLevelEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() { return value; }
    public String getLabel() { return label; }

    public static MasteryLevelEnum of(int value) {
        for (MasteryLevelEnum e : values()) {
            if (e.value == value) return e;
        }
        return WEAK;
    }

    public static int calculateLevel(double masteryRate) {
        if (masteryRate >= 0.8) return GOOD.value;
        if (masteryRate >= 0.6) return FAIR.value;
        return WEAK.value;
    }

    public static String getLabel(int level) {
        return of(level).label;
    }
}
