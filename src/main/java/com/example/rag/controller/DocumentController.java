package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.entity.Document;
import com.example.rag.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** 上传文档（txt/md），同步解析入库 */
    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(file));
    }

    /** 文档列表 */
    @GetMapping
    public Result<List<Document>> list() {
        return Result.success(documentService.listAll());
    }

    /** 删除文档 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.success();
    }
}
