package com.comprehensive.eureka.chatbot.langchain.session;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final Duration MEMORY_TTL = Duration.ofHours(24);
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<String> entries = redisTemplate.opsForList().range(key(memoryId), 0, -1);
        if (entries == null) return List.of();
        return entries.stream().map(this::decode).toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = key(memoryId);
        redisTemplate.delete(key);
        if (messages.isEmpty()) return;
        redisTemplate.opsForList().rightPushAll(key, messages.stream().map(this::encode).toList());
        redisTemplate.expire(key, MEMORY_TTL);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        redisTemplate.delete(key(memoryId));
    }

    private String key(Object memoryId) {
        return "chatbot:room:" + memoryId + ":memory";
    }

    private String encode(ChatMessage message) {
        String text;
        if (message instanceof SystemMessage systemMessage) {
            text = systemMessage.text();
        } else if (message instanceof UserMessage userMessage) {
            text = userMessage.singleText();
        } else if (message instanceof AiMessage aiMessage) {
            text = aiMessage.text();
        } else {
            throw new IllegalArgumentException("텍스트 기반 대화 메시지만 저장할 수 있습니다: " + message.type());
        }
        return message.type().name() + "|" + java.util.Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private ChatMessage decode(String entry) {
        String[] parts = entry.split("\\|", 2);
        if (parts.length != 2) throw new IllegalStateException("손상된 챗봇 메모리 데이터입니다.");
        String text = new String(java.util.Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return switch (parts[0]) {
            case "SYSTEM" -> SystemMessage.from(text);
            case "USER" -> UserMessage.from(text);
            case "AI" -> AiMessage.from(text);
            default -> throw new IllegalStateException("지원하지 않는 챗봇 메시지 유형입니다: " + parts[0]);
        };
    }
}
