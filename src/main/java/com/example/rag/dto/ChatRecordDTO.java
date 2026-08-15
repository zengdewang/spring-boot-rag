package com.example.rag.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话记录展示对象（citations 已解析为结构化列表）。
 */
@Data
public class ChatRecordDTO {

    private Long id;
    private String sessionId;
    private String question;
    private String answer;
    private List<Citation> citations;
    private LocalDateTime createdAt;
}
