package com.example.rag.service.document;

import com.example.rag.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    /** 上传文档：解析文本并入库 */
    Document upload(MultipartFile file);

    /** 文档列表（不含全文） */
    List<Document> listAll();

    /** 删除文档 */
    void delete(Long id);
}
