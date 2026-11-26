package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.support.RedisContainerSupport;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisChatMemoryStoreTest extends RedisContainerSupport {

    private final StringRedisTemplate redisTemplate = newRedisTemplate();
    private final RedisChatMemoryStore store = new RedisChatMemoryStore(redisTemplate);

    @BeforeEach
    void setUp() {
        redisTemplate.delete("chatbot:room:1:memory");
    }

    @Test
    void 저장한_대화_문맥을_새_인스턴스에서도_순서대로_복원한다() {
        List<ChatMessage> messages = List.of(
                SystemMessage.from("요금제 추천 프롬프트"),
                UserMessage.from("데이터 많이 쓰는 요금제|추천"),
                AiMessage.from("5G 프리미어를 추천합니다"));

        store.updateMessages(1L, messages);

        RedisChatMemoryStore restarted = new RedisChatMemoryStore(redisTemplate);
        assertThat(restarted.getMessages(1L)).containsExactlyElementsOf(messages);
        assertThat(redisTemplate.getExpire("chatbot:room:1:memory")).isPositive();
    }

    @Test
    void 갱신하면_이전_문맥을_대체한다() {
        store.updateMessages(1L, List.of(UserMessage.from("첫 질문"), AiMessage.from("첫 답변")));

        store.updateMessages(1L, List.of(SystemMessage.from("새 프롬프트")));

        assertThat(store.getMessages(1L)).containsExactly(SystemMessage.from("새 프롬프트"));
    }

    @Test
    void 빈_문맥으로_갱신하면_키를_삭제한다() {
        store.updateMessages(1L, List.of(UserMessage.from("질문")));

        store.updateMessages(1L, List.of());

        assertThat(redisTemplate.hasKey("chatbot:room:1:memory")).isFalse();
        assertThat(store.getMessages(1L)).isEmpty();
    }
}
