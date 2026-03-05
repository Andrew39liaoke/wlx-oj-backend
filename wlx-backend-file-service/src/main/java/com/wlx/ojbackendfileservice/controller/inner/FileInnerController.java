package com.wlx.ojbackendfileservice.controller.inner;

import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendfileservice.service.FileInfoService;
import com.wlx.ojbackendserviceclient.service.FileFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 内部文件服务控制器
 */
@RestController
@RequestMapping("/inner")
@Slf4j
public class FileInnerController implements FileFeignClient {

    @Resource
    private FileInfoService fileInfoService;

    @Override
    @PostMapping("/upload")
    @Operation(summary = "内部上传文件")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "biz", defaultValue = "default") String biz,
            @RequestParam("userId") Long userId) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "用户ID不能为空");
        }

        try {
            FileUploadResponse response = fileInfoService.uploadFile(file, biz, userId);
            return Result.success(response);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    @Override
    @PostMapping("/delete")
    @Operation(summary = "内部删除文件")
    public ResponseEntity<Boolean> deleteFile(
            @RequestParam("fileId") Long fileId,
            @RequestParam("userId") Long userId) {

        if (fileId == null || fileId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件ID不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "用户ID不能为空");
        }

        boolean result = fileInfoService.deleteFile(fileId, userId);
        return Result.success(result);
    }

    @Override
    @GetMapping("/info")
    @Operation(summary = "内部获取文件信息")
    public ResponseEntity<com.wlx.ojbackendmodel.model.entity.FileInfo> getFileInfo(@RequestParam("fileId") Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件ID不能为空");
        }

        com.wlx.ojbackendmodel.model.entity.FileInfo fileInfo = fileInfoService.getFileInfo(fileId);
        if (fileInfo == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR, "文件不存在");
        }

        return Result.success(fileInfo);
    }
}
