package com.example.rag.service.document;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器：固定窗口切分，每块 500 字符，相邻块间重叠 50 字符，
 * 避免语义在切分边界处断裂。
 */
@Component
public class TextChunker {

    public static final int CHUNK_SIZE = 500;
    public static final int OVERLAP = 50;

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + CHUNK_SIZE, length);
            chunks.add(text.substring(start, end));
            if (end == length) {
                break;
            }
            start = end - OVERLAP;
        }
        return chunks;
    }
}
