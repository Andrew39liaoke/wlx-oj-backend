package com.wlx.ojbackendaiservice.utils;

import com.alibaba.fastjson.JSON;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 工具类，适配 wlx-oj-backend
 */
@Slf4j
public class OkHttpUtils {
    private static volatile OkHttpClient okHttpClient = null;
    private static volatile Semaphore semaphore = null;
    private Map<String, String> headerMap;
    private Map<String, String> paramMap;
    private String url;
    private Request.Builder request;

    private OkHttpUtils() {
        if (okHttpClient == null) {
            synchronized (OkHttpUtils.class) {
                if (okHttpClient == null) {
                    okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(20, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
    }

    private static Semaphore getSemaphoreInstance() {
        synchronized (OkHttpUtils.class) {
            if (semaphore == null) {
                semaphore = new Semaphore(0);
            }
        }
        return semaphore;
    }

    public static OkHttpUtils builder() {
        return new OkHttpUtils();
    }

    public OkHttpUtils url(String url) {
        this.url = url;
        return this;
    }

    public OkHttpUtils addParam(String key, String value) {
        if (paramMap == null) {
            paramMap = new LinkedHashMap<>(16);
        }
        paramMap.put(key, value);
        return this;
    }

    public OkHttpUtils addHeader(String key, String value) {
        if (headerMap == null) {
            headerMap = new LinkedHashMap<>(16);
        }
        headerMap.put(key, value);
        return this;
    }

    public OkHttpUtils get() {
        request = new Request.Builder().get();
        StringBuilder urlBuilder = new StringBuilder(url);
        if (paramMap != null) {
            urlBuilder.append("?");
            try {
                for (Map.Entry<String, String> entry : paramMap.entrySet()) {
                    urlBuilder.append(URLEncoder.encode(entry.getKey(), "utf-8")).
                            append("=").
                            append(URLEncoder.encode(entry.getValue(), "utf-8")).
                            append("&");
                }
            } catch (Exception e) {
                log.error("URL编码失败", e);
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        request.url(urlBuilder.toString());
        return this;
    }

    public OkHttpUtils post(boolean isJsonPost) {
        RequestBody requestBody;
        if (isJsonPost) {
            String json = "";
            if (paramMap != null) {
                json = JSON.toJSONString(paramMap);
            } 
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        } else {
            FormBody.Builder formBody = new FormBody.Builder();
            if (paramMap != null) {
                paramMap.forEach(formBody::add);
            }
            requestBody = formBody.build();
        }
        request = new Request.Builder().post(requestBody).url(url);
        return this;
    }

    public OkHttpUtils postRaw(String body, String mediaType) {
        MediaType contentType = MediaType.parse(mediaType);
        RequestBody requestBody = RequestBody.create(body, contentType);
        request = new Request.Builder().post(requestBody).url(url);
        return this;
    }

    public String sync() {
        setHeader(request);
        try {
            Response response = okHttpClient.newCall(request.build()).execute();
            ThrowUtils.throwIf(response.body() == null, ResopnseCodeEnum.API_REQUEST_ERROR, "接口响应为空");
            return response.body().string();
        } catch (IOException e) {
            log.error("http请求失败：{}", e.getMessage());
            throw new BusinessException(ResopnseCodeEnum.API_REQUEST_ERROR, "接口调用失败");
        }
    }

    public String async() {
        StringBuilder buffer = new StringBuilder("");
        setHeader(request);
        okHttpClient.newCall(request.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                buffer.append("请求出错：").append(e.getMessage());
                getSemaphoreInstance().release();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) {
                    buffer.append(response.body().string());
                }
                getSemaphoreInstance().release();
            }
        });
        try {
            getSemaphoreInstance().acquire();
        } catch (InterruptedException e) {
            log.error("线程等待中断", e);
            Thread.currentThread().interrupt();
        }
        return buffer.toString();
    }

    private void setHeader(Request.Builder request) {
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }
}
