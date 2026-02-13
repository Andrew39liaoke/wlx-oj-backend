package com.wlx.ojbackendfileservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendmodel.model.entity.FileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件信息服务
 */
public interface FileInfoService extends IService<FileInfo> {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param biz 业务类型
     * @param userId 用户ID
     * @return 文件上传响应
     */
    FileUploadResponse uploadFile(MultipartFile file, String biz, Long userId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteFile(Long fileId, Long userId);

    /**
     * 根据文件ID获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    FileInfo getFileInfo(Long fileId);

    /**
     * 根据文件路径删除OSS中的文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    boolean deleteOSSFile(String filePath);
}
