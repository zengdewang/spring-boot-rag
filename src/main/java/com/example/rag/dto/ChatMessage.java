package com.example.rag.dto;

/**
 * 对话消息（OpenAI/DeepSeek 兼容格式）。
 */
public record ChatMessage(String role, String content) {
}
