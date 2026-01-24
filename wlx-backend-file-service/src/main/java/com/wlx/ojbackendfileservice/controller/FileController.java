package com.wlx.ojbackendfileservice.controller;

import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendfileservice.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/file")
@Tag(name = "FileController", description = "文件管理接口")
@Slf4j
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public ResponseEntity<com.wlx.ojbackendcommon.common.ResponseEntity<String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "fileName", required = false) String fileName) {

        if (file.isEmpty()) {
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.PARAMS_ERROR));
        }

        try {
            // 如果没有指定文件名，使用原始文件名
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = file.getOriginalFilename();
            }

            String fileUrl = fileService.uploadFile(file, fileName);
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.SUCCESS.getCode(),
                    fileService.getFileUrl(fileUrl), "文件上传成功"));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.OPERATION_ERROR));
        }
    }

    @GetMapping("/download/{fileName}")
    @Operation(summary = "下载文件")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable String fileName) {
        try {
            InputStream inputStream = fileService.downloadFile(fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete/{fileName}")
    @Operation(summary = "删除文件")
    public ResponseEntity<com.wlx.ojbackendcommon.common.ResponseEntity<Boolean>> deleteFile(@PathVariable String fileName) {
        try {
            boolean result = fileService.deleteFile(fileName);
            if (result) {
                return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.SUCCESS.getCode(),
                        true, "文件删除成功"));
            } else {
                return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.OPERATION_ERROR.getCode(),
                        false, "文件删除失败"));
            }
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.OPERATION_ERROR));
        }
    }

    @GetMapping("/url/{fileName}")
    @Operation(summary = "获取文件访问URL")
    public ResponseEntity<com.wlx.ojbackendcommon.common.ResponseEntity<String>> getFileUrl(@PathVariable String fileName) {
        try {
            String fileUrl = fileService.getFileUrl(fileName);
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.SUCCESS.getCode(),
                    fileUrl, "获取文件URL成功"));
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            return ResponseEntity.ok(new com.wlx.ojbackendcommon.common.ResponseEntity<>(ResopnseCodeEnum.OPERATION_ERROR));
        }
    }
}
