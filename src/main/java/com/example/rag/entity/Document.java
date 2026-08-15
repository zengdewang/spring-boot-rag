package com.example.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体。
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档名称 */
    private String name;

    /** 文件类型: txt / md */
    private String fileType;

    /** 解析后的全文 */
    private String content;

    /** 分块数量 */
    private Integer chunkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
