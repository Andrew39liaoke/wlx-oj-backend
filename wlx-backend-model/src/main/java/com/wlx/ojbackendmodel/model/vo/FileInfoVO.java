package com.wlx.ojbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件信息视图
 */
@Data
public class FileInfoVO implements Serializable {

    /**
     * id
     */
    private Long id;

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
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
