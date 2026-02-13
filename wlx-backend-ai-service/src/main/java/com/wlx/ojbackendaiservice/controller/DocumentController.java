package com.wlx.ojbackendaiservice.controller;

import com.wlx.ojbackendaiservice.service.document.DocumentProcessService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    @Resource
    private DocumentProcessService documentProcessService;

    public DocumentController(DocumentProcessService documentProcessService) {
        this.documentProcessService = documentProcessService;
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tags", required = false) String tags) {

        try {
            log.info("收到文档上传请求: {}", file.getOriginalFilename());

            // 准备元数据
            Map<String, Object> metadata = new HashMap<>();
            if (category != null) {
                metadata.put("category", category);
            }
            if (tags != null) {
                metadata.put("tags", tags);
            }
            metadata.put("upload_time", System.currentTimeMillis());

            // 处理文档
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            String result = documentProcessService.processAndStoreDocument(resource, metadata);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("文档上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("文档上传失败: " + e.getMessage()));
        }
    }

    @Data
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public static ApiResponse success(Object data) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(true);
            response.setData(data);
            return response;
        }

        public static ApiResponse error(String message) {
            ApiResponse response = new ApiResponse();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
}
