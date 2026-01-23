package com.wlx.ojbackendcommon.common;

/**
 * 返回工具类
 */
public class Result {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> success(T data) {
        return new ResponseEntity<>(0, data, "ok");
    }

    /**
     * 认证成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> authSuccess(T data) {
        return new ResponseEntity<>(ResopnseCodeEnum.AUTH_SUCCESS.getCode(), data, ResopnseCodeEnum.AUTH_SUCCESS.getMessage());
    }

    /**
     * 认证失败
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> authFailure(T data) {
        return new ResponseEntity<>(ResopnseCodeEnum.AUTH_FAILURE.getCode(), data, ResopnseCodeEnum.AUTH_FAILURE.getMessage());
    }

    /**
     * 失败
     *
     * @param resopnseCodeEnum
     * @return
     */
    public static ResponseEntity error(ResopnseCodeEnum resopnseCodeEnum) {
        return new ResponseEntity<>(resopnseCodeEnum);
    }

    /**
     * 失败
     *
     * @param code
     * @param message
     * @return
     */
    public static ResponseEntity error(int code, String message) {
        return new ResponseEntity(code, null, message);
    }

    /**
     * 失败
     *
     * @param resopnseCodeEnum
     * @return
     */
    public static ResponseEntity error(ResopnseCodeEnum resopnseCodeEnum, String message) {
        return new ResponseEntity(resopnseCodeEnum.getCode(), null, message);
    }
}
