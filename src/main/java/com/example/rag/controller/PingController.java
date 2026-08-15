package com.example.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口，用于验证应用能否正常启动。
 */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    @GetMapping
    public Map<String, Object> ping() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "pong");
        result.put("time", LocalDateTime.now());
        return result;
    }
}
