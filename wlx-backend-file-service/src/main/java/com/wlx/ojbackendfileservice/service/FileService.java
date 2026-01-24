package com.wlx.ojbackendfileservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件服务接口
 */
public interface FileService {

    /**
     * 上传文件
     * @param file 文件
     * @param fileName 文件名
     * @return 文件访问URL
     */
    String uploadFile(MultipartFile file, String fileName);

    /**
     * 下载文件
     * @param fileName 文件名
     * @return 文件输入流
     */
    InputStream downloadFile(String fileName);

    /**
     * 删除文件
     * @param fileName 文件名
     * @return 是否删除成功
     */
    boolean deleteFile(String fileName);

    /**
     * 获取文件访问URL
     * @param fileName 文件名
     * @return 文件访问URL
     */
    String getFileUrl(String fileName);
}
