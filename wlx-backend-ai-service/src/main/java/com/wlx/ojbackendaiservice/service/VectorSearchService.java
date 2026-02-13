package com.wlx.ojbackendaiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class VectorSearchService {

    private final VectorStore vectorStore;

    public VectorSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 基础相似度搜索
     */
    public List<Document> search(String query, int topK) {
        log.info("执行搜索: query='{}', topK={}", query, topK);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build()
        );

        log.info("找到 {} 个相关文档", results.size());
        return results;
    }

    /**
     * 带相似度阈值的搜索
     */
    public List<Document> searchWithThreshold(String query, int topK, double threshold) {
        log.info("执行搜索: query='{}', topK={}, threshold={}", query, topK, threshold);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK).similarityThreshold(threshold)
                        .build()
        );

        log.info("找到 {} 个相关文档（相似度 >= {}）", results.size(), threshold);
        return results;
    }
}
