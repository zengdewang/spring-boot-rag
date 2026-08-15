package com.example.rag.service.document;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.common.exception.BusinessException;
import com.example.rag.entity.Document;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.embedding.EmbeddingService;
import com.example.rag.service.vector.MilvusVectorStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentParser documentParser;
    private final TextChunker textChunker;
    private final EmbeddingService embeddingService;
    private final MilvusVectorStore milvusVectorStore;

    @Override
    public Document upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "文件名不能为空");
        }
        String content = documentParser.parse(file);

        Document doc = new Document();
        doc.setName(name);
        doc.setFileType(extractFileType(name));
        doc.setContent(content);
        doc.setChunkCount(0);
        documentMapper.insert(doc);

        // 同步流水线：分块 → 向量化 → 入库 Milvus
        try {
            List<String> chunks = textChunker.chunk(content);
            if (!chunks.isEmpty()) {
                List<List<Float>> vectors = embeddingService.embedBatch(chunks);
                milvusVectorStore.insert(doc.getId(), chunks, vectors);
                doc.setChunkCount(chunks.size());
                documentMapper.updateById(doc);
            }
        } catch (Exception e) {
            // 流水线中途失败则回滚文档记录，避免脏数据
            documentMapper.deleteById(doc.getId());
            throw e;
        }
        return doc;
    }

    @Override
    public List<Document> listAll() {
        // 只查列表字段，不把全文 LONGTEXT 带出来
        return documentMapper.selectList(
                Wrappers.<Document>lambdaQuery()
                        .select(Document::getId, Document::getName,
                                Document::getFileType, Document::getChunkCount,
                                Document::getCreatedAt)
                        .orderByDesc(Document::getId));
    }

    @Override
    public void delete(Long id) {
        Document doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在");
        }
        milvusVectorStore.deleteByDocId(id);
        documentMapper.deleteById(id);
    }

    private String extractFileType(String name) {
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "txt" : name.substring(idx + 1).toLowerCase();
    }
}
