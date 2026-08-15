package com.example.rag.dto;

import lombok.Data;

import java.util.List;

/**
 * 对话响应。
 */
@Data
public class ChatResponse {

    private String sessionId;

    /** 模型回答 */
    private String answer;

    /** 引用来源（检索到的知识块） */
    private List<Citation> citations;
}
