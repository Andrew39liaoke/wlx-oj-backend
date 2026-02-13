package com.wlx.ojbackendaiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdvancedSearchService {

    private final VectorStore vectorStore;

    public AdvancedSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 按文档类型搜索
     */
    public List<Document> searchByDocumentType(String query, String docType, int topK) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

}
