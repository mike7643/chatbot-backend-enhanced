package com.comprehensive.eureka.chatbot.langchain.session;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatSessionStateService {

    private static final long SESSION_TTL_HOURS = 24;
    private static final String FIRST_CHAT_ACTIVATED = "firstChatActivated";
    private static final String PROMPT_PROCESSING = "promptProcessing";

    private final StringRedisTemplate redisTemplate;

    public boolean isFirstMessage(Long chatRoomId) {
        String key = stateKey(chatRoomId);
        Object value = redisTemplate.opsForHash().get(key, FIRST_CHAT_ACTIVATED);
        refreshTtl(key);
        return value == null || Boolean.parseBoolean(value.toString());
    }

    public void markFirstMessageHandled(Long chatRoomId) {
        put(chatRoomId, FIRST_CHAT_ACTIVATED, false);
    }

    public boolean isPromptProcessing(Long chatRoomId) {
        Object value = redisTemplate.opsForHash().get(stateKey(chatRoomId), PROMPT_PROCESSING);
        return value != null && Boolean.parseBoolean(value.toString());
    }

    public void setPromptProcessing(Long chatRoomId, boolean processing) {
        put(chatRoomId, PROMPT_PROCESSING, processing);
    }

    private void put(Long chatRoomId, String field, boolean value) {
        String key = stateKey(chatRoomId);
        redisTemplate.opsForHash().put(key, field, Boolean.toString(value));
        refreshTtl(key);
    }

    private void refreshTtl(String key) {
        redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    private String stateKey(Long chatRoomId) {
        return "chatbot:room:" + chatRoomId + ":state";
    }
}
