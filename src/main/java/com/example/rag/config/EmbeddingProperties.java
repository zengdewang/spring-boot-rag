package com.example.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 配置：对应 application.yml 中的 llm.embedding.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.embedding")
public class EmbeddingProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private int dimension;
}
