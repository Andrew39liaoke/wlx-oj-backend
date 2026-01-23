package com.wlx.ojbackendcommon.exception;


import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;

/**
 * 自定义异常类
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResopnseCodeEnum resopnseCodeEnum) {
        super(resopnseCodeEnum.getMessage());
        this.code = resopnseCodeEnum.getCode();
    }

    public BusinessException(ResopnseCodeEnum resopnseCodeEnum, String message) {
        super(message);
        this.code = resopnseCodeEnum.getCode();
    }

    public int getCode() {
        return code;
    }
}
