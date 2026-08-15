package com.example.rag.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenAI 兼容 Embedding 接口响应。
 * 示例：{"data":[{"embedding":[0.1,0.2,...],"index":0}],"model":"BAAI/bge-m3","object":"list","usage":{...}}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse {

    private List<EmbeddingData> data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbeddingData {
        private List<Float> embedding;
        private Integer index;
    }
}
