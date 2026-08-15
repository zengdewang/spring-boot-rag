package com.example.rag.service.chat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.rag.dto.ChatRecordDTO;
import com.example.rag.dto.Citation;
import com.example.rag.dto.SessionSummary;
import com.example.rag.entity.ChatRecord;
import com.example.rag.mapper.ChatRecordMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 对话记录存取：保存每轮问答、查询最近历史（多轮对话用）。
 */
@Service
@RequiredArgsConstructor
public class ChatRecordService {

    private final ChatRecordMapper chatRecordMapper;
    private final ObjectMapper objectMapper;

    /** 保存一轮问答 */
    public void save(String sessionId, String question, String answer, List<Citation> citations) {
        ChatRecord record = new ChatRecord();
        record.setSessionId(sessionId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setCitations(toJson(citations));
        chatRecordMapper.insert(record);
    }

    /** 查询某会话最近的 N 轮记录（按时间正序返回） */
    public List<ChatRecordDTO> recentHistory(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        List<ChatRecord> records = chatRecordMapper.selectList(
                Wrappers.<ChatRecord>lambdaQuery()
                        .eq(ChatRecord::getSessionId, sessionId)
                        .orderByDesc(ChatRecord::getId)
                        .last("LIMIT " + limit));
        Collections.reverse(records);
        return records.stream().map(this::toDTO).toList();
    }

    /** 会话列表（按最近活跃排序，含轮数与最后问题预览） */
    public List<SessionSummary> listSessions() {
        return chatRecordMapper.listSessionSummaries();
    }

    /** 删除整个会话的历史记录 */
    public void deleteSession(String sessionId) {
        chatRecordMapper.delete(Wrappers.<ChatRecord>lambdaQuery()
                .eq(ChatRecord::getSessionId, sessionId));
    }

    private ChatRecordDTO toDTO(ChatRecord record) {
        ChatRecordDTO dto = new ChatRecordDTO();
        dto.setId(record.getId());
        dto.setSessionId(record.getSessionId());
        dto.setQuestion(record.getQuestion());
        dto.setAnswer(record.getAnswer());
        dto.setCitations(parseCitations(record.getCitations()));
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }

    private String toJson(List<Citation> citations) {
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<Citation> parseCitations(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Citation>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
