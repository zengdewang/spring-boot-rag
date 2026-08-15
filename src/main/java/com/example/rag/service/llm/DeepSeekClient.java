package com.example.rag.service.llm;

import com.example.rag.common.exception.BusinessException;
import com.example.rag.config.DeepSeekProperties;
import com.example.rag.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型客户端：封装 /chat/completions（OpenAI 兼容接口）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekClient {

    private final DeepSeekProperties props;
    private final RestClient.Builder restClientBuilder;
    private RestClient restClient;

    @PostConstruct
    void init() {
        this.restClient = restClientBuilder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }

    /**
     * 非流式对话，返回助手回复文本。
     */
    public String chat(List<ChatMessage> messages) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("stream", false);
        body.put("max_tokens", 1024);

        try {
            JsonNode root = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                throw new BusinessException(500, "DeepSeek 响应为空");
            }
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(500, "DeepSeek 未返回有效内容");
            }
            return content.asText();
        } catch (RestClientException e) {
            log.error("调用 DeepSeek 失败: model={}", props.getModel(), e);
            throw new BusinessException(500, "调用 DeepSeek 服务失败: " + e.getMessage());
        }
    }
}
