package com.example.rag.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    private final MilvusProperties props;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig config = ConnectConfig.builder()
                .uri("http://" + props.getHost() + ":" + props.getPort())
                .build();
        return new MilvusClientV2(config);
    }
}
