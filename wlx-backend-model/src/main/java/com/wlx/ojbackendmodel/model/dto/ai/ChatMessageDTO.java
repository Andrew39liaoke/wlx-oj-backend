package com.wlx.ojbackendmodel.model.dto.ai;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息DTO - 对应Redis中存储的消息格式
 */
@Data
public class ChatMessageDTO {

    /**
     * 消息类型：USER / ASSISTANT
     */
    private String messageType;

    /**
     * 消息文本内容
     */
    private String textContent;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 媒体附件列表
     */
    private List<Object> media;
}
