package com.wlx.ojbackendfileservice.service.impl;

import cn.hutool.core.io.FileUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wlx.ojbackendfileservice.config.FileUploadConfig;
import com.wlx.ojbackendfileservice.config.OSSConfig;
import com.wlx.ojbackendfileservice.mapper.FileInfoMapper;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendmodel.model.entity.FileInfo;
import com.wlx.ojbackendfileservice.service.FileInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

/**
 * 文件信息服务实现
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Resource
    private OSS ossClient;

    @Resource
    private FileUploadConfig fileUploadConfig;

    @Resource
    private OSSConfig ossConfig;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String biz, Long userId) {
        // 1. 验证文件
        validateFile(file);

        // 2. 生成文件路径
        String filePath = generateFilePath(file.getOriginalFilename(), biz);

        try {
            // 3. 上传到OSS
            uploadToOSS(file, filePath);

            // 4. 保存文件信息到数据库
            FileInfo fileInfo = saveFileInfo(file, filePath, userId);

            // 5. 返回响应
            return buildUploadResponse(fileInfo);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            // 如果上传失败，删除OSS中的文件
            try {
                deleteOSSFile(filePath);
            } catch (Exception ex) {
                log.error("删除OSS文件失败", ex);
            }
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(Long fileId, Long userId) {
        // 获取文件信息
        FileInfo fileInfo = getById(fileId);
        if (fileInfo == null) {
            return false;
        }

        // 检查权限（只能删除自己的文件）
        if (!fileInfo.getUserId().equals(userId)) {
            throw new RuntimeException("无权限删除此文件");
        }

        // 删除OSS中的文件
        boolean ossDeleted = deleteOSSFile(fileInfo.getFilePath());

        // 删除数据库记录
        boolean dbDeleted = removeById(fileId);

        return ossDeleted && dbDeleted;
    }

    @Override
    public FileInfo getFileInfo(Long fileId) {
        return getById(fileId);
    }

    @Override
    public boolean deleteOSSFile(String filePath) {
        try {
            ossClient.deleteObject(ossConfig.getBucketName(), filePath);
            return true;
        } catch (Exception e) {
            log.error("删除OSS文件失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > fileUploadConfig.getMaxSize()) {
            throw new RuntimeException("文件大小超过限制：" + fileUploadConfig.getMaxSize() + "字节");
        }

        // 检查文件类型
        String contentType = file.getContentType();
        if (contentType == null || !fileUploadConfig.getAllowedTypes().contains(contentType)) {
            throw new RuntimeException("不支持的文件类型：" + contentType);
        }
    }

    /**
     * 生成文件路径
     */
    private String generateFilePath(String originalFilename, String biz) {
        // 获取文件扩展名
        String extension = FileUtil.getSuffix(originalFilename);

        // 生成唯一文件名
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        // 构建路径：前缀/业务类型/年月/文件名
        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        return fileUploadConfig.getPathPrefix() + biz + "/" + datePath + "/" + fileName;
    }

    /**
     * 上传到OSS
     */
    private void uploadToOSS(MultipartFile file, String filePath) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest putObjectRequest = new PutObjectRequest(
                ossConfig.getBucketName(),
                filePath,
                file.getInputStream(),
                metadata
        );

        ossClient.putObject(putObjectRequest);
    }

    /**
     * 保存文件信息到数据库
     */
    private FileInfo saveFileInfo(MultipartFile file, String filePath, Long userId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setFilePath(filePath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(file.getContentType());
        fileInfo.setFileUrl(fileUploadConfig.getUrlPrefix() + filePath);
        fileInfo.setUserId(userId);
        fileInfo.setCreateTime(new Date());

        save(fileInfo);
        return fileInfo;
    }

    /**
     * 构建上传响应
     */
    private FileUploadResponse buildUploadResponse(FileInfo fileInfo) {
        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(fileInfo.getId());
        response.setUrl(fileInfo.getFileUrl());
        response.setFileName(fileInfo.getFileName());
        response.setFileSize(fileInfo.getFileSize());
        return response;
    }
}
