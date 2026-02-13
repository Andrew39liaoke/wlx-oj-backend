package com.wlx.ojbackendaiservice.service.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentLoaderService {

    /**
     * 加载 PDF 文档
     */
    public List<Document> loadPdfDocument(Resource resource) {
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        .withNumberOfBottomTextLinesToDelete(3)
                        .withNumberOfTopPagesToSkipBeforeDelete(1)
                        .build())
                .withPagesPerDocument(1)
                .build();

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, config);
        return pdfReader.get();
    }

    /**
     * 加载多种格式文档（使用 Tika）
     */
    public List<Document> loadDocument(Resource resource) {
        TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
        return tikaReader.get();
    }

    /**
     * 分割文档为小块
     */
    public List<Document> splitDocuments(List<Document> documents, int chunkSize, int overlap) {
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, overlap, 5, 10000, true);
        return splitter.apply(documents);
    }
}
