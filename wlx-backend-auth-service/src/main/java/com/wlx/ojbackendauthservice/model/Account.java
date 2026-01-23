package com.wlx.ojbackendauthservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信配置实体，绑定 application.yml 中的 tencent.wechat
 */
@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "tencent.wechat")
public class Account {
    /**
     * 微信公众平台测试号的appID
     */
    private String appId;

    /**
     * 微信公众平台测试号的appsecret
     */
    private String appSecret;

    /**
     * 微信公众平台网页登录服务->网页帐号的授权回调页面域名(要带http://前缀)
     */
    private String domain;

    /**
     * 本地处理扫码结果的controller方法访问地址
     */
    private String redirectUri;
}


