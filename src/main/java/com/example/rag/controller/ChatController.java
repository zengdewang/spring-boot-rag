package com.example.rag.controller;

import com.example.rag.common.Result;
import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.service.llm.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** 知识库问答：返回答案 + 引用来源 */
    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.success(chatService.answer(request));
    }
}
