package com.wlx.ojbackendfileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置
 */
@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageConfig {
    /**
     * 存储类型：local（本地）、oss（阿里云）、cos（腾讯云）、minio
     */
    private String type = "local";

    /**
     * 本地存储配置
     */
    private LocalConfig local = new LocalConfig();

    /**
     * 阿里云OSS配置
     */
    private OssConfig oss = new OssConfig();

    /**
     * 腾讯云COS配置
     */
    private CosConfig cos = new CosConfig();

    /**
     * MinIO配置
     */
    private MinioConfig minio = new MinioConfig();

    @Data
    public static class LocalConfig {
        /**
         * 本地存储路径
         */
        private String path = "./upload";
    }

    @Data
    public static class OssConfig {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;
    }

    @Data
    public static class CosConfig {
        private String secretId;
        private String secretKey;
        private String region;
        private String bucketName;
    }

    @Data
    public static class MinioConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;
    }
}
