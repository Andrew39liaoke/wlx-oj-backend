package com.wlx.ojbackendcommon.utils;

import com.alibaba.fastjson.JSON;
import com.wlx.ojbackendcommon.common.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * HTTP 响应工具
 */
public final class ResponseUtil {

    private ResponseUtil() {
    }

    /**
     * 将 BaseResponse 对象以 JSON 格式写入 HttpServletResponse
     *
     * @param response Http 响应对象
     * @param model    要返回的响应实体（BaseResponse）
     * @throws IOException 当写出失败时抛出
     */
    public static void print(HttpServletResponse response, ResponseEntity<?> model) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setCharacterEncoding("utf-8");
        PrintWriter writer = response.getWriter();
        String json = JSON.toJSONString(model);
        writer.println(json);
        writer.flush();
    }
}


