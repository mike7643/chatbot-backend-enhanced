package com.comprehensive.eureka.chatbot.langchain.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionStateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ChatSessionStateService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        service = new ChatSessionStateService(redisTemplate);
    }

    @Test
    void treatsAnUninitializedRoomAsTheFirstMessageAndRefreshesItsTtl() {
        Long chatRoomId = 101L;
        when(hashOperations.get("chatbot:room:101:state", "firstChatActivated")).thenReturn(null);

        boolean firstMessage = service.isFirstMessage(chatRoomId);

        assertThat(firstMessage).isTrue();
        verify(redisTemplate).expire("chatbot:room:101:state", 24, TimeUnit.HOURS);
    }

    @Test
    void persistsPromptProcessingByChatRoomWithoutTouchingAnotherRoom() {
        service.setPromptProcessing(101L, true);
        service.setPromptProcessing(202L, false);

        verify(hashOperations).put("chatbot:room:101:state", "promptProcessing", "true");
        verify(hashOperations).put("chatbot:room:202:state", "promptProcessing", "false");
    }
}
