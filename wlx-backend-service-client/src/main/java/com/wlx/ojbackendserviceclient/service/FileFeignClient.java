package com.wlx.ojbackendserviceclient.service;

import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendmodel.model.entity.FileInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 Feign 客户端 (内部调用)
 */
@FeignClient(name = "wlx-backend-file-service", path = "/api/file/inner")
public interface FileFeignClient {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param biz 业务类型
     * @param userId 用户ID
     * @return 文件上传响应
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FileUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "biz", defaultValue = "default") String biz,
            @RequestParam("userId") Long userId
    );

    /**
     * 获取文件信息
     *
     * @param fileId 文件ID
     * @return 文件信息
     */
    @GetMapping("/info")
    ResponseEntity<FileInfo> getFileInfo(@RequestParam("fileId") Long fileId);

    /**
     * 删除文件
     *
     * @param fileId 文件ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    ResponseEntity<Boolean> deleteFile(
            @RequestParam("fileId") Long fileId,
            @RequestParam("userId") Long userId
    );
}
