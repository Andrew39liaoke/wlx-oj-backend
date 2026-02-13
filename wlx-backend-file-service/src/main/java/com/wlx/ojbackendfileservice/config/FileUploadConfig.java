package com.wlx.ojbackendfileservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 文件上传配置
 */
@Configuration
@ConfigurationProperties(prefix = "file.upload")
@Data
public class FileUploadConfig {

    /**
     * 允许的文件类型
     */
    private List<String> allowedTypes;

    /**
     * 最大文件大小（字节）
     */
    private Long maxSize;

    /**
     * 文件存储路径前缀
     */
    private String pathPrefix;

    /**
     * 文件访问URL前缀
     */
    private String urlPrefix;
}
