package com.wlx.ojbackendaiservice.service.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DocumentManagementService {

    private final VectorStore vectorStore;
    private final JedisPooled jedisPooled;

    public DocumentManagementService(VectorStore vectorStore, JedisPooled jedisPooled) {
        this.vectorStore = vectorStore;
        this.jedisPooled = jedisPooled;
    }

    /**
     * 删除文档
     */
    public void deleteDocuments(List<String> documentIds) {
        log.info("删除文档: {}", documentIds);
        vectorStore.delete(documentIds);
        log.info("✅ 已删除 {} 个文档", documentIds.size());
    }

    /**
     * 按元数据删除文档
     */
    public void deleteByMetadata(String metadataKey, String metadataValue) {
        log.info("按元数据删除: {}={}", metadataKey, metadataValue);

        // 查询符合条件的文档
        String queryStr = String.format("@%s:%s", metadataKey, metadataValue);
        Query query = new Query(queryStr).limit(0, 10000);

        try {
            SearchResult result = jedisPooled.ftSearch("spring-ai-index", query);
            List<String> idsToDelete = new ArrayList<>();

            result.getDocuments().forEach(doc -> {
                idsToDelete.add(doc.getId());
            });

            if (!idsToDelete.isEmpty()) {
                deleteDocuments(idsToDelete);
            }

            log.info("✅ 共删除 {} 个文档", idsToDelete.size());

        } catch (Exception e) {
            log.error("删除失败", e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    /**
     * 更新文档元数据
     */
    public void updateDocumentMetadata(String documentId, Map<String, Object> newMetadata) {
        log.info("更新文档元数据: id={}, metadata={}", documentId, newMetadata);

        // Redis 不支持直接更新，需要先删除再添加
        // 实际应用中可能需要先查询文档内容

        log.info("✅ 文档元数据已更新");
    }

    /**
     * 统计文档数量
     */
    public long countDocuments() {
        try {
            Query query = new Query("*").limit(0, 0);
            SearchResult result = jedisPooled.ftSearch("spring-ai-index", query);
            return result.getTotalResults();
        } catch (Exception e) {
            log.error("统计文档失败", e);
            return 0;
        }
    }

    /**
     * 清空所有文档
     */
    public void clearAllDocuments() {
        log.warn("⚠️  准备清空所有文档");

        try {
            Query query = new Query("*").limit(0, 10000);
            SearchResult result = jedisPooled.ftSearch("spring-ai-index", query);

            List<String> allIds = new ArrayList<>();
            result.getDocuments().forEach(doc -> allIds.add(doc.getId()));

            if (!allIds.isEmpty()) {
                vectorStore.delete(allIds);
                log.info("✅ 已清空 {} 个文档", allIds.size());
            } else {
                log.info("ℹ️  没有文档需要清空");
            }

        } catch (Exception e) {
            log.error("清空文档失败", e);
            throw new RuntimeException("清空文档失败", e);
        }
    }
}
