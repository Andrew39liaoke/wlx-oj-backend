package com.wlx.ojbackendmodel.model.vo.live;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 弹幕消息VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DanmakuMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String content;

    private String color;

    private Integer size;

    private Long ts;

    private Integer role;
}
