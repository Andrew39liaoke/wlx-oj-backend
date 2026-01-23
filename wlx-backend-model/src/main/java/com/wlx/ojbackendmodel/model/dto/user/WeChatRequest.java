package com.wlx.ojbackendmodel.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeChatRequest {
    /*签名*/
    private String signature;

    /*时间戳*/
    private String timestamp;

    /*随机数*/
    private String nonce;

    /*随机字符串*/
    private String echostr;
}
