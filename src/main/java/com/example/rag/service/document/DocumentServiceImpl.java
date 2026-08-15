package com.example.rag.service.document;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.common.exception.BusinessException;
import com.example.rag.entity.Document;
import com.example.rag.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentParser documentParser;

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

        // TODO Day 3：接入 分块 → Embedding → Milvus 入库 流水线
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
        // TODO Day 3：同时删除 Milvus 中该文档对应的向量
        documentMapper.deleteById(id);
    }

    private String extractFileType(String name) {
        int idx = name.lastIndexOf('.');
        return idx < 0 ? "txt" : name.substring(idx + 1).toLowerCase();
    }
}
