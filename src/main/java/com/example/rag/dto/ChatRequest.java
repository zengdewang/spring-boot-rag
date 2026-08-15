package com.example.rag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对话请求。
 */
@Data
public class ChatRequest {

    /** 会话 ID：多轮对话用，首次可不传（Day 6 生效） */
    private String sessionId;

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;
}
