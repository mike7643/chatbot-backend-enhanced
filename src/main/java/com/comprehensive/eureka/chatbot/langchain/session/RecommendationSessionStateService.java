package com.comprehensive.eureka.chatbot.langchain.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationSessionStateService {

    private static final long SESSION_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Long chatRoomId, RecommendationSessionContext context) {
        try {
            redisTemplate.opsForValue().set(
                    recommendationKey(chatRoomId),
                    objectMapper.writeValueAsString(context),
                    SESSION_TTL_HOURS,
                    TimeUnit.HOURS
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("추천 세션 정보를 저장할 수 없습니다.", e);
        }
    }

    public Optional<RecommendationSessionContext> find(Long chatRoomId) {
        String key = recommendationKey(chatRoomId);
        String serializedContext = redisTemplate.opsForValue().get(key);
        if (serializedContext == null) {
            return Optional.empty();
        }

        try {
            RecommendationSessionContext context = objectMapper.readValue(serializedContext, RecommendationSessionContext.class);
            redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
            return Optional.of(context);
        } catch (JsonProcessingException e) {
            log.warn("손상된 추천 세션 정보를 삭제합니다. chatRoomId={}", chatRoomId, e);
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void clear(Long chatRoomId) {
        redisTemplate.delete(recommendationKey(chatRoomId));
    }

    private String recommendationKey(Long chatRoomId) {
        return "chatbot:room:" + chatRoomId + ":recommendation";
    }
}
