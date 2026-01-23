package com.wlx.ojbackendcommon.exception;


import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;

/**
 * 抛异常工具类
 */
public class ThrowUtils {

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resopnseCodeEnum
     */
    public static void throwIf(boolean condition, ResopnseCodeEnum resopnseCodeEnum) {
        throwIf(condition, new BusinessException(resopnseCodeEnum));
    }

    /**
     * 条件成立则抛异常
     *
     * @param condition
     * @param resopnseCodeEnum
     * @param message
     */
    public static void throwIf(boolean condition, ResopnseCodeEnum resopnseCodeEnum, String message) {
        throwIf(condition, new BusinessException(resopnseCodeEnum, message));
    }
}
