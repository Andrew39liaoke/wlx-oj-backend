package com.wlx.ojbackendfileservice.controller;

import com.wlx.ojbackendcommon.common.ResponseEntity;
import com.wlx.ojbackendcommon.common.Result;
import com.wlx.ojbackendcommon.exception.BusinessException;
import com.wlx.ojbackendcommon.common.ResopnseCodeEnum;
import com.wlx.ojbackendcommon.utils.JwtUtil;
import com.wlx.ojbackendmodel.model.dto.file.FileUploadResponse;
import com.wlx.ojbackendmodel.model.entity.FileInfo;
import com.wlx.ojbackendmodel.model.entity.User;
import com.wlx.ojbackendfileservice.service.FileInfoService;

import com.wlx.ojbackendserviceclient.service.PostFeignClient;
import com.wlx.ojbackendserviceclient.service.UserFeignClient;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/")
@Slf4j
public class FileController {

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private PostFeignClient postFeignClient;



    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "biz", defaultValue = "default") String biz,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }
        // 获取当前登录用户
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        if (loginUser == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        try {
            FileUploadResponse response = fileInfoService.uploadFile(file, biz, loginUser.getId());
            return Result.success(response);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete")
    @Operation(summary = "删除文件")
    public ResponseEntity<Boolean> deleteFile(
            @RequestParam("fileId") Long fileId,
            HttpServletRequest request) {

        if (fileId == null || fileId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        if (loginUser == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        boolean result = fileInfoService.deleteFile(fileId, loginUser.getId());
        return Result.success(result);
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取文件信息")
    public ResponseEntity<FileInfo> getFileInfo(@RequestParam("fileId") Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件ID不能为空");
        }

        FileInfo fileInfo = fileInfoService.getFileInfo(fileId);
        if (fileInfo == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_FOUND_ERROR, "文件不存在");
        }

        return Result.success(fileInfo);
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像")
    public ResponseEntity<Boolean> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }

        // 获取当前登录用户
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        if (loginUser == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        try {
            // 上传头像文件
            FileUploadResponse uploadResponse = fileInfoService.uploadFile(file, "avatar", loginUser.getId());
            if (uploadResponse == null || StringUtils.isBlank(uploadResponse.getUrl())) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "头像上传失败");
            }

            // 更新用户头像
            boolean updateResult = userFeignClient.updateUserAvatar(loginUser.getId(), uploadResponse.getUrl());
            if (!updateResult) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "头像更新失败");
            }

            return Result.success(true);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "头像上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传帖子封面
     */
    @PostMapping("/upload/post/cover")
    @Operation(summary = "上传帖子封面")
    public ResponseEntity<Boolean> uploadPostCover(
            @RequestParam("file") MultipartFile file,
            @RequestParam("postId") Long postId,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }

        if (postId == null || postId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "帖子ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        if (loginUser == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        try {
            // 上传封面文件
            FileUploadResponse uploadResponse = fileInfoService.uploadFile(file, "post_cover", loginUser.getId());
            if (uploadResponse == null || StringUtils.isBlank(uploadResponse.getUrl())) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "封面上传失败");
            }

            // 更新帖子封面
            boolean updateResult = postFeignClient.updatePostCover(postId, uploadResponse.getUrl());
            if (!updateResult) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "封面更新失败");
            }

            return Result.success(true);
        } catch (Exception e) {
            log.error("帖子封面上传失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "帖子封面上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传班级文件（仅限 PDF）
     */
    @PostMapping("/upload/class/file")
    @Operation(summary = "上传班级文件（仅限 PDF）")
    public ResponseEntity<Boolean> uploadClassFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("classId") Long classId,
            HttpServletRequest request) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "文件不能为空");
        }

        if (classId == null || classId <= 0) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "班级ID不能为空");
        }

        // 获取当前登录用户
        User loginUser = userFeignClient.getLoginUser(request.getHeader(JwtUtil.HEADER));
        if (loginUser == null) {
            throw new BusinessException(ResopnseCodeEnum.NOT_LOGIN_ERROR);
        }

        // 限制只能上传 PDF 文件
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename) || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException(ResopnseCodeEnum.PARAMS_ERROR, "只支持 PDF 格式的文件");
        }

        try {
            // 上传文件
            FileUploadResponse uploadResponse = fileInfoService.uploadFile(file, "class_file", loginUser.getId());
            if (uploadResponse == null || StringUtils.isBlank(uploadResponse.getUrl())) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "文件上传失败");
            }

            // 更新班级的文件信息ID
            boolean updateResult = userFeignClient.updateClassFileInfoId(classId, uploadResponse.getFileId());
            if (!updateResult) {
                throw new BusinessException(ResopnseCodeEnum.OPERATION_ERROR, "班级文件信息更新失败");
            }

            return Result.success(true);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("班级文件上传失败", e);
            throw new BusinessException(ResopnseCodeEnum.SYSTEM_ERROR, "班级文件上传失败：" + e.getMessage());
        }
    }
}
