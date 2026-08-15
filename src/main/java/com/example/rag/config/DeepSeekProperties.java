package com.example.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 配置：对应 application.yml 中的 llm.deepseek.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm.deepseek")
public class DeepSeekProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private int timeoutSeconds;
}
