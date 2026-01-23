package com.wlx.ojbackendmodel.model.wx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 微信用户信息实体（对应微信 userinfo 返回字段）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class wxUser {
    private String openid;
    private String nickname;
    private String sex;
    private String province;
    private String city;
    private String country;
    private String headimgurl;
    private List<String> privilege;
    private String unionid;
}




