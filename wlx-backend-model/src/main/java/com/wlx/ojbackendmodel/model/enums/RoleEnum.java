package com.wlx.ojbackendmodel.model.enums;

public enum RoleEnum {

    ADMIN("admin", 1),
    USER("user", 2);

    private final String key;
    private final int value;

    RoleEnum(String key, int value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }

    public static RoleEnum fromKey(String key) {
        if (key == null) return null;
        for (RoleEnum e : values()) {
            if (e.key.equals(key)) return e;
        }
        return null;
    }

    public static RoleEnum fromValue(int value) {
        for (RoleEnum e : values()) {
            if (e.value == value) return e;
        }
        return null;
    }
}
