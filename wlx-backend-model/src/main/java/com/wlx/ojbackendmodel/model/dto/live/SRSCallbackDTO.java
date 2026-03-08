package com.wlx.ojbackendmodel.model.dto.live;

import lombok.Data;

import java.io.Serializable;

/**
 * SRS 回调 DTO
 */
@Data
public class SRSCallbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String action;

    private String clientId;

    private String ip;

    private String vhost;

    private String app;

    private String stream;

    private String param;

    private String serverId;
    private String streamUrl;
    private String streamId;
}
