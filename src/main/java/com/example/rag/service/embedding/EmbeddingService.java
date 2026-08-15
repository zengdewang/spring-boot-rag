package com.example.rag.service.embedding;

import java.util.List;

/**
 * Embedding 服务抽象。当前用硅基流动（OpenAI 兼容接口），
 * 未来可替换为本地部署（TEI / Xinference / Ollama），只需新增实现类。
 */
public interface EmbeddingService {

    /** 单条文本向量化 */
    List<Float> embed(String text);

    /** 批量文本向量化（返回顺序与入参一致） */
    List<List<Float>> embedBatch(List<String> texts);

    /** 向量维度（bge-m3 = 1024） */
    int dimension();
}
