package com.wlx.ojbackendcommon.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 */
@Data
public class ResponseEntity<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public ResponseEntity(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public ResponseEntity(int code, T data) {
        this(code, data, "");
    }

    public ResponseEntity(ResopnseCodeEnum resopnseCodeEnum) {
        this(resopnseCodeEnum.getCode(), null, resopnseCodeEnum.getMessage());
    }
}
