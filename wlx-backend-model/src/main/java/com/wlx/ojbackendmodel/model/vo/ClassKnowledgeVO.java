package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 班级知识库视图对象
 */
@Data
public class ClassKnowledgeVO implements Serializable {

    /**
     * 知识库记录ID
     */
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 文件信息ID
     */
    private Long fileInfoId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
