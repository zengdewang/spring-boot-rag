package com.example.rag.service.llm;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.dto.ChatMessage;
import com.example.rag.dto.ChatRecordDTO;
import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.dto.Citation;
import com.example.rag.entity.Document;
import com.example.rag.mapper.DocumentMapper;
import com.example.rag.service.chat.ChatRecordService;
import com.example.rag.service.embedding.EmbeddingService;
import com.example.rag.service.vector.MilvusVectorStore;
import io.milvus.v2.service.vector.response.SearchResp.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RAG 问答核心服务：
 * 问题向量化 → Milvus 检索 TopK → 拼接多轮历史 → 组装 Prompt → DeepSeek 生成 → 保存记录 → 返回答案 + 引用。
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TOP_K = 3;
    private static final int HISTORY_LIMIT = 5;

    private final EmbeddingService embeddingService;
    private final MilvusVectorStore milvusVectorStore;
    private final DeepSeekClient deepSeekClient;
    private final DocumentMapper documentMapper;
    private final ChatRecordService chatRecordService;

    public ChatResponse answer(ChatRequest request) {
        // 会话 ID：未传则自动生成并返回给客户端
        String sessionId = (request.getSessionId() == null || request.getSessionId().isBlank())
                ? UUID.randomUUID().toString()
                : request.getSessionId();

        // 1. 问题向量化
        List<Float> queryVector = embeddingService.embed(request.getQuestion());

        // 2. Milvus 检索 TopK
        List<SearchResult> results = milvusVectorStore.search(queryVector, TOP_K);

        // 3. 组装引用
        List<Citation> citations = buildCitations(results);

        // 4. 拼接最近 5 轮历史 + 组装 Prompt
        List<ChatRecordDTO> history = chatRecordService.recentHistory(sessionId, HISTORY_LIMIT);
        List<ChatMessage> messages = buildMessages(request.getQuestion(), citations, history);

        // 5. 调用 DeepSeek 生成回答
        String answer = deepSeekClient.chat(messages);

        // 6. 保存对话记录
        chatRecordService.save(sessionId, request.getQuestion(), answer, citations);

        // 7. 返回
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setAnswer(answer);
        response.setCitations(citations);
        return response;
    }

    private List<Citation> buildCitations(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        List<Long> docIds = results.stream()
                .map(r -> ((Number) r.getEntity().get("doc_id")).longValue())
                .distinct()
                .toList();
        Map<Long, String> idToName = documentMapper.selectList(
                        Wrappers.<Document>lambdaQuery()
                                .select(Document::getId, Document::getName)
                                .in(Document::getId, docIds))
                .stream()
                .collect(Collectors.toMap(Document::getId, Document::getName));

        return results.stream().map(r -> {
            Map<String, Object> entity = r.getEntity();
            Long docId = ((Number) entity.get("doc_id")).longValue();
            Citation c = new Citation();
            c.setDocId(docId);
            c.setDocName(idToName.getOrDefault(docId, "未知文档"));
            c.setText((String) entity.get("text"));
            c.setScore(r.getScore());
            return c;
        }).toList();
    }

    private List<ChatMessage> buildMessages(String question, List<Citation> citations,
                                            List<ChatRecordDTO> history) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "你是一个严谨的企业知识库问答助手。"
                + "1. 回答问题时优先依据【参考资料】，不要编造事实；"
                + "2. 如果问题能在对话历史中找到答案（例如询问之前提到的内容），可直接引用历史；"
                + "3. 若参考资料和对话历史中都没有相关信息，请如实回答「知识库中暂未找到相关信息」；"
                + "4. 回答简洁、条理清晰。"));

        // 多轮对话历史（最近 5 轮）
        for (ChatRecordDTO record : history) {
            messages.add(new ChatMessage("user", record.getQuestion()));
            messages.add(new ChatMessage("assistant", record.getAnswer()));
        }

        StringBuilder user = new StringBuilder();
        if (!citations.isEmpty()) {
            user.append("【参考资料】\n");
            for (int i = 0; i < citations.size(); i++) {
                Citation c = citations.get(i);
                user.append('[').append(i + 1).append("] (来源文档《")
                        .append(c.getDocName()).append("》)\n")
                        .append(c.getText()).append("\n\n");
            }
        }
        user.append("【用户问题】\n").append(question);
        messages.add(new ChatMessage("user", user.toString()));
        return messages;
    }
}
