package com.wlx.ojbackendcommon.common;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    //代码调试
    public static final String NO_LANGUAGE = "没有此类编程语言";

    //权限问题
    public static final String ACCESS_DENIED = "你没有权限访问此接口";
    public static final String  TOKEN_FAILURE  =  "token失效";
   public static final String DATA_ACCESS_DENIED = "您无权访问此数据";

    //班级
    public static final String INVITATIONCODE_NOT_FOUND = "班级邀请码不存在";
    public static final String EXIT_FAILURE_NOT_EXIST_CLASS = "退出班级失败，包含不存在的班级";
    public static final String CLASS_AND_TEACHER_NOT_FOUND = "当前老师班级内没有该班级";
    public static final String CLASS_AND_STUDENT_NOT_FOUND = "当前学生不在该班级";

    public static final String INVALID_FORMAT_FAILURE = "输入格式有误";
    public static final String DATA_NOT_FOUND = "数据不存在";

    //题目
    public static final String PROBLEM_NOT_FOUND = "题目不存在";

    //AI
    public static final String AI_CHAT_ID_NOT_FOUND = "没有发现会话id";
    public static final String AI_CHAT_TYPE_NOT_FOUND = "没有发现会话类型";
    public static final String AI_CHAT_ID_NOT_STANDARD = "ChatId 不符合格式";

}
