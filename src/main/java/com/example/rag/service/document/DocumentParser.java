package com.example.rag.service.document;

import com.example.rag.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 文档解析器：从上传文件中抽取纯文本。
 * MVP 仅支持 txt / md（纯文本类），直接按 UTF-8 读取。
 */
@Component
public class DocumentParser {

    public String parse(MultipartFile file) {
        String name = file.getOriginalFilename();
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
            throw new BusinessException(400, "暂不支持的文件类型，仅支持 txt / md");
        }
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(500, "读取文件内容失败: " + e.getMessage());
        }
    }
}
