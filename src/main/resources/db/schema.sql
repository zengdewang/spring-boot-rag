-- =============================================================
-- RAG 知识库问答系统 建表脚本 (MySQL 8.0)
-- 目标数据库: rag_kb
-- 执行方式: docker exec -i rag-mysql mysql -uroot -proot123456 rag_kb < schema.sql
-- =============================================================

-- 文档表：记录上传的文档及解析后的全文（用于分块入库和引用溯源）
CREATE TABLE IF NOT EXISTS `document` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(255) NOT NULL COMMENT '文档名称',
    `file_type`   VARCHAR(20)  NOT NULL DEFAULT 'txt' COMMENT '文件类型: txt/md',
    `content`     LONGTEXT     NULL COMMENT '解析后的全文',
    `chunk_count` INT          NOT NULL DEFAULT 0 COMMENT '分块数量',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档';

-- 对话记录表：保存每轮问答，用于多轮对话历史拼接
CREATE TABLE IF NOT EXISTS `chat_record` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID（多轮对话分组）',
    `question`   TEXT        NULL COMMENT '用户问题',
    `answer`     TEXT        NULL COMMENT '模型回答',
    `citations`  JSON        NULL COMMENT '引用来源（检索到的 chunk 文本列表）',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话记录';
