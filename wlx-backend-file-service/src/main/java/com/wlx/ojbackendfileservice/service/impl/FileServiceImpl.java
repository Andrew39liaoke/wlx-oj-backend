package com.wlx.ojbackendfileservice.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.wlx.ojbackendfileservice.config.FileStorageConfig;
import com.wlx.ojbackendfileservice.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件服务实现类
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private FileStorageConfig fileStorageConfig;

    @Override
    public String uploadFile(MultipartFile file, String fileName) {
        try {
            // 生成唯一文件名
            String uniqueFileName = generateUniqueFileName(fileName);

            switch (fileStorageConfig.getType().toLowerCase()) {
                case "local":
                    return uploadToLocal(file, uniqueFileName);
                case "oss":
                    return uploadToOss(file, uniqueFileName);
                case "cos":
                    return uploadToCos(file, uniqueFileName);
                case "minio":
                    return uploadToMinio(file, uniqueFileName);
                default:
                    throw new IllegalArgumentException("Unsupported storage type: " + fileStorageConfig.getType());
            }
        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String fileName) {
        try {
            switch (fileStorageConfig.getType().toLowerCase()) {
                case "local":
                    return downloadFromLocal(fileName);
                case "oss":
                    return downloadFromOss(fileName);
                case "cos":
                    return downloadFromCos(fileName);
                case "minio":
                    return downloadFromMinio(fileName);
                default:
                    throw new IllegalArgumentException("Unsupported storage type: " + fileStorageConfig.getType());
            }
        } catch (Exception e) {
            log.error("File download failed", e);
            throw new RuntimeException("File download failed: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            switch (fileStorageConfig.getType().toLowerCase()) {
                case "local":
                    return deleteFromLocal(fileName);
                case "oss":
                    return deleteFromOss(fileName);
                case "cos":
                    return deleteFromCos(fileName);
                case "minio":
                    return deleteFromMinio(fileName);
                default:
                    throw new IllegalArgumentException("Unsupported storage type: " + fileStorageConfig.getType());
            }
        } catch (Exception e) {
            log.error("File delete failed", e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String fileName) {
        switch (fileStorageConfig.getType().toLowerCase()) {
            case "local":
                return getLocalFileUrl(fileName);
            case "oss":
                return getOssFileUrl(fileName);
            case "cos":
                return getCosFileUrl(fileName);
            case "minio":
                return getMinioFileUrl(fileName);
            default:
                throw new IllegalArgumentException("Unsupported storage type: " + fileStorageConfig.getType());
        }
    }

    // 本地存储相关方法
    private String uploadToLocal(MultipartFile file, String fileName) throws IOException {
        Path uploadPath = Paths.get(fileStorageConfig.getLocal().getPath());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);
        return fileName;
    }

    private InputStream downloadFromLocal(String fileName) throws IOException {
        Path filePath = Paths.get(fileStorageConfig.getLocal().getPath(), fileName);
        return Files.newInputStream(filePath);
    }

    private boolean deleteFromLocal(String fileName) {
        Path filePath = Paths.get(fileStorageConfig.getLocal().getPath(), fileName);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Delete local file failed", e);
            return false;
        }
    }

    private String getLocalFileUrl(String fileName) {
        return "/api/file/download/" + fileName;
    }

    // 阿里云OSS相关方法
    private String uploadToOss(MultipartFile file, String fileName) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(
                fileStorageConfig.getOss().getEndpoint(),
                fileStorageConfig.getOss().getAccessKeyId(),
                fileStorageConfig.getOss().getAccessKeySecret());

        try {
            ossClient.putObject(fileStorageConfig.getOss().getBucketName(), fileName, file.getInputStream());
            return fileName;
        } finally {
            ossClient.shutdown();
        }
    }

    private InputStream downloadFromOss(String fileName) {
        OSS ossClient = new OSSClientBuilder().build(
                fileStorageConfig.getOss().getEndpoint(),
                fileStorageConfig.getOss().getAccessKeyId(),
                fileStorageConfig.getOss().getAccessKeySecret());

        try {
            OSSObject ossObject = ossClient.getObject(fileStorageConfig.getOss().getBucketName(), fileName);
            return ossObject.getObjectContent();
        } finally {
            ossClient.shutdown();
        }
    }

    private boolean deleteFromOss(String fileName) {
        OSS ossClient = new OSSClientBuilder().build(
                fileStorageConfig.getOss().getEndpoint(),
                fileStorageConfig.getOss().getAccessKeyId(),
                fileStorageConfig.getOss().getAccessKeySecret());

        try {
            ossClient.deleteObject(fileStorageConfig.getOss().getBucketName(), fileName);
            return true;
        } catch (Exception e) {
            log.error("Delete OSS file failed", e);
            return false;
        } finally {
            ossClient.shutdown();
        }
    }

    private String getOssFileUrl(String fileName) {
        return "https://" + fileStorageConfig.getOss().getBucketName() + "." +
               fileStorageConfig.getOss().getEndpoint().replace("https://", "") + "/" + fileName;
    }

    // 腾讯云COS相关方法
    private String uploadToCos(MultipartFile file, String fileName) throws IOException {
        COSCredentials cred = new BasicCOSCredentials(
                fileStorageConfig.getCos().getSecretId(),
                fileStorageConfig.getCos().getSecretKey());

        Region region = new Region(fileStorageConfig.getCos().getRegion());
        ClientConfig clientConfig = new ClientConfig(region);

        COSClient cosClient = new COSClient(cred, clientConfig);

        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    fileStorageConfig.getCos().getBucketName(),
                    fileName,
                    file.getInputStream(),
                    null);
            cosClient.putObject(putObjectRequest);
            return fileName;
        } finally {
            cosClient.shutdown();
        }
    }

    private InputStream downloadFromCos(String fileName) {
        // COS下载实现（简化版）
        throw new UnsupportedOperationException("COS download not implemented yet");
    }

    private boolean deleteFromCos(String fileName) {
        // COS删除实现（简化版）
        throw new UnsupportedOperationException("COS delete not implemented yet");
    }

    private String getCosFileUrl(String fileName) {
        return "https://" + fileStorageConfig.getCos().getBucketName() + ".cos." +
               fileStorageConfig.getCos().getRegion() + ".myqcloud.com/" + fileName;
    }

    // MinIO相关方法
    private String uploadToMinio(MultipartFile file, String fileName) throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(fileStorageConfig.getMinio().getEndpoint())
                .credentials(fileStorageConfig.getMinio().getAccessKey(), fileStorageConfig.getMinio().getSecretKey())
                .build();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(fileStorageConfig.getMinio().getBucketName())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        return fileName;
    }

    private InputStream downloadFromMinio(String fileName) {
        // MinIO下载实现（简化版）
        throw new UnsupportedOperationException("MinIO download not implemented yet");
    }

    private boolean deleteFromMinio(String fileName) {
        // MinIO删除实现（简化版）
        throw new UnsupportedOperationException("MinIO delete not implemented yet");
    }

    private String getMinioFileUrl(String fileName) {
        return fileStorageConfig.getMinio().getEndpoint() + "/" +
               fileStorageConfig.getMinio().getBucketName() + "/" + fileName;
    }

    /**
     * 生成唯一文件名
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}
