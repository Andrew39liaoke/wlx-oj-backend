package com.wlx.ojbackendmodel.model.dto.file;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 */
@Data
public class FileUploadResponse implements Serializable {

    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    private static final long serialVersionUID = 1L;
}
