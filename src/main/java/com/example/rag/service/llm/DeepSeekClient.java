package com.example.rag.service.llm;

import com.example.rag.common.exception.BusinessException;
import com.example.rag.config.DeepSeekProperties;
import com.example.rag.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DeepSeek 大模型客户端：封装 /chat/completions（OpenAI 兼容接口）。
 * 非流式走 RestClient；流式走 JDK HttpClient（流式读取更稳定）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekClient {

    private final DeepSeekProperties props;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
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
        Map<String, Object> body = buildBody(messages, false);
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

    /**
     * 流式对话：每产生一段内容增量就回调 onDelta，读到 [DONE] 结束。
     * 使用 JDK HttpClient 的 InputStream 流式读取，逐行解析 SSE。
     */
    public void streamChat(List<ChatMessage> messages, Consumer<String> onDelta) {
        Map<String, Object> body = buildBody(messages, true);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String err = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException(500, "DeepSeek 返回状态码 " + response.statusCode() + ": " + err);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith(":") || !line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode node = objectMapper.readTree(data);
                    // v4 模型会先流 reasoning_content（思维链），此时 content 为 JSON null，需跳过
                    JsonNode content = node.path("choices").path(0).path("delta").get("content");
                    if (content != null && content.isTextual()) {
                        String text = content.asText();
                        if (!text.isEmpty()) {
                            onDelta.accept(text);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "调用 DeepSeek 流式服务被中断");
        } catch (IOException e) {
            log.error("调用 DeepSeek 流式接口失败: model={}", props.getModel(), e);
            throw new BusinessException(500, "调用 DeepSeek 流式服务失败: " + e.getMessage());
        }
    }

    private Map<String, Object> buildBody(List<ChatMessage> messages, boolean stream) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("messages", messages);
        body.put("stream", stream);
        body.put("max_tokens", 1024);
        return body;
    }
}
