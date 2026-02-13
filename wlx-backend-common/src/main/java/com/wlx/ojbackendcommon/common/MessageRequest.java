package com.wlx.ojbackendcommon.common;

import lombok.Data;

@Data
public class MessageRequest {
    private String msg;
    private Long userId;
}
