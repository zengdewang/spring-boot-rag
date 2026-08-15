package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对话记录实体。
 */
@Data
@TableName("chat_record")
public class ChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话 ID（多轮对话分组） */
    private String sessionId;

    /** 用户问题 */
    private String question;

    /** 模型回答 */
    private String answer;

    /** 引用来源（List<Citation> 的 JSON 字符串） */
    private String citations;

    private LocalDateTime createdAt;
}
