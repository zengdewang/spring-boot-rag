package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.dto.ChatRecordDTO;
import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.dto.SessionSummary;
import com.example.rag.service.chat.ChatRecordService;
import com.example.rag.service.llm.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRecordService chatRecordService;
    private final ThreadPoolTaskExecutor chatExecutor;

    /** 知识库问答：多轮对话，返回答案 + 引用来源 */
    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.answer(request));
    }

    /** 流式问答（SSE）：内容增量实时推送，前端打字机效果 */
    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(120_000L);
        chatExecutor.execute(() -> chatService.streamAnswer(request, emitter));
        return emitter;
    }

    /** 查询会话历史记录 */
    @GetMapping("/history/{sessionId}")
    public Result<List<ChatRecordDTO>> history(@PathVariable String sessionId) {
        return Result.success(chatRecordService.recentHistory(sessionId, 50));
    }

    /** 会话列表（按最近活跃排序） */
    @GetMapping("/sessions")
    public Result<List<SessionSummary>> sessions() {
        return Result.success(chatRecordService.listSessions());
    }

    /** 删除会话及其全部对话记录 */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        chatRecordService.deleteSession(sessionId);
        return Result.success();
    }
}
