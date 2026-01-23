package com.wlx.ojbackendmodel.model.wx;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信授权返回的 Token 实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Token {
    private String access_token;
    private String expires_in;
    private String refresh_token;
    private String openid;
    private String scope;
    private String is_snapshotuser;
    private String unionid;
}


