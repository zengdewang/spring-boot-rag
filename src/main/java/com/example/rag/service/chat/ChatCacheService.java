package com.example.rag.service.chat;

import com.example.rag.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 高频问题答案缓存：以问题 MD5 为 key 缓存 ChatResponse，TTL 1 小时。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCacheService {

    private static final String KEY_PREFIX = "rag:chat:";
    private static final Duration TTL = Duration.ofHours(1);

    private final RedisTemplate<String, Object> redisTemplate;

    /** 查缓存，未命中返回 null */
    public ChatResponse get(String question) {
        Object value = redisTemplate.opsForValue().get(buildKey(question));
        if (value instanceof ChatResponse response) {
            log.info("对话缓存命中: question={}", question);
            return response;
        }
        return null;
    }

    /** 写入缓存 */
    public void put(String question, ChatResponse response) {
        redisTemplate.opsForValue().set(buildKey(question), response, TTL);
        log.info("对话写入缓存: question={}, ttlSeconds={}", question, TTL.getSeconds());
    }

    private String buildKey(String question) {
        return KEY_PREFIX + DigestUtils.md5DigestAsHex(question.getBytes(StandardCharsets.UTF_8));
    }
}
