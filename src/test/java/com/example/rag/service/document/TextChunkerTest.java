package com.example.rag.service.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void splitsLongTextWithOverlap() {
        // 1050 字符 → 3 块：500 + 500 + 150，第二块从 450 开始（重叠 50）
        String text = "a".repeat(1050);
        List<String> chunks = chunker.chunk(text);

        assertEquals(3, chunks.size());
        assertEquals(text.substring(0, 500), chunks.get(0));
        assertEquals(text.substring(450, 950), chunks.get(1));
        assertEquals(text.substring(900, 1050), chunks.get(2));
    }

    @Test
    void shortTextIsSingleChunk() {
        List<String> chunks = chunker.chunk("hello world");
        assertEquals(1, chunks.size());
        assertEquals("hello world", chunks.get(0));
    }

    @Test
    void blankTextReturnsEmpty() {
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   ").isEmpty());
    }
}
