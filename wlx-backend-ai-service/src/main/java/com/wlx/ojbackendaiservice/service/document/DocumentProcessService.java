package com.wlx.ojbackendaiservice.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DocumentProcessService {

    private final DocumentLoaderService loaderService;
    private final VectorStore vectorStore;

    @Value("${app.document.chunk-size:800}")
    private int chunkSize;

    @Value("${app.document.chunk-overlap:200}")
    private int chunkOverlap;

    public DocumentProcessService(DocumentLoaderService loaderService, VectorStore vectorStore) {
        this.loaderService = loaderService;
        this.vectorStore = vectorStore;
    }

    /**
     * 处理并存储文档
     */
    public String processAndStoreDocument(Resource resource, Map<String, Object> metadata) {
        try {
            log.info("开始处理文档: {}", resource.getFilename());

            // 1. 加载文档
            List<Document> documents = loaderService.loadDocument(resource);
            log.info("加载了 {} 个文档页面", documents.size());

            // 2. 添加元数据
            documents.forEach(doc -> {
                Map<String, Object> meta = new HashMap<>(metadata);
                meta.put("filename", resource.getFilename());
                meta.put("page", doc.getMetadata().get("page"));
                doc.getMetadata().putAll(meta);
            });

            // 3. 分割文档
            List<Document> chunks = loaderService.splitDocuments(documents, chunkSize, chunkOverlap);
            log.info("文档分割成 {} 个块", chunks.size());

            // 4. 存储到向量库
            vectorStore.add(chunks);
            log.info("✅ 文档已成功存储到向量库");

            return String.format("成功处理文档，共 %d 个块", chunks.size());

        } catch (Exception e) {
            log.error("处理文档失败", e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量处理文档
     */
    public Map<String, String> processBatchDocuments(List<Resource> resources, Map<String, Object> commonMetadata) {
        Map<String, String> results = new HashMap<>();

        for (Resource resource : resources) {
            try {
                String result = processAndStoreDocument(resource, commonMetadata);
                results.put(resource.getFilename(), result);
            } catch (Exception e) {
                results.put(resource.getFilename(), "失败: " + e.getMessage());
            }
        }

        return results;
    }
}
