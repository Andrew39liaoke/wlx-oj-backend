package com.wlx.ojbackendcommon.common;

/**
 * 自定义错误码
 */
public enum ResopnseCodeEnum {

    SUCCESS(0, "ok"),
    AUTH_SUCCESS(200, "认证成功"),
    AUTH_FAILURE(401, "认证失败"),
    USER_NOT_EXIST(40102, "用户名不存在"),
    USERNAME_OR_PASSWORD_ERROR(40103, "用户名或密码错误"),
    LOGIN_FAILED(40104, "登录失败"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败"),
    BAD_REQUEST(400, "失败的请求"),
    API_REQUEST_ERROR(50010, "接口调用失败"),

    STATE_ERROR(2000,"UUID错误");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    ResopnseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
