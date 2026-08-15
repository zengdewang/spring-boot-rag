package com.example.rag.service.embedding;

import com.example.rag.common.exception.BusinessException;
import com.example.rag.config.EmbeddingProperties;
import com.example.rag.dto.EmbeddingResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 硅基流动 SiliconFlow Embedding 实现（OpenAI 兼容接口）。
 * 调用 POST {base-url}/embeddings，模型 BAAI/bge-m3（1024 维）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiliconFlowEmbeddingService implements EmbeddingService {

    private final EmbeddingProperties props;
    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = restClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    @Override
    public List<Float> embed(String text) {
        List<List<Float>> vectors = embedBatch(List.of(text));
        if (vectors.isEmpty()) {
            throw new BusinessException(500, "Embedding 返回为空");
        }
        return vectors.get(0);
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("input", texts);
        body.put("encoding_format", "float");

        try {
            EmbeddingResponse response = restClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                throw new BusinessException(500, "Embedding 响应解析失败");
            }
            List<List<Float>> vectors = new ArrayList<>();
            for (EmbeddingResponse.EmbeddingData data : response.getData()) {
                vectors.add(data.getEmbedding());
            }
            return vectors;
        } catch (RestClientException e) {
            log.error("调用 Embedding API 失败: baseUrl={}", props.getBaseUrl(), e);
            throw new BusinessException(500, "调用 Embedding 服务失败: " + e.getMessage());
        }
    }

    @Override
    public int dimension() {
        return props.getDimension();
    }
}
