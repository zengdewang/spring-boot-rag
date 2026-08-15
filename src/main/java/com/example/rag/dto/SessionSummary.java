package com.example.rag.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话摘要：用于历史会话列表展示。
 */
@Data
public class SessionSummary {

    private String sessionId;

    /** 消息轮数 */
    private Long messageCount;

    /** 最近活跃时间 */
    private LocalDateTime lastTime;

    /** 最后一条问题预览 */
    private String lastQuestion;
}
