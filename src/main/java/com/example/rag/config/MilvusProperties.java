package com.example.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Milvus 配置：对应 application.yml 中的 milvus.*
 */
@Data
@Component
@ConfigurationProperties(prefix = "milvus")
public class MilvusProperties {

    private String host;
    private int port;
    private String collection;
}
