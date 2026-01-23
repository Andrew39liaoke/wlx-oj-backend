package com.wlx.ojbackendgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "white-list")
public class WhiteList {

    private List<String> urls = new ArrayList<>();

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    /**
     * 返回白名单路径数组（仅路径）
     */
    public String[] getRequestMatchers() {
        if (urls == null) {
            return new String[0];
        }
        return urls.toArray(new String[0]);
    }
}


