package com.example.rag.dto;

import lombok.Data;

/**
 * 引用来源：一条检索到的知识块。
 */
@Data
public class Citation {

    /** 来源文档 ID */
    private Long docId;

    /** 来源文档名 */
    private String docName;

    /** 命中的知识块文本 */
    private String text;

    /** 相似度得分 */
    private Float score;
}
