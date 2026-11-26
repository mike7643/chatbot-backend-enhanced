package com.comprehensive.eureka.chatbot.langchain.integration.service;

import com.comprehensive.eureka.chatbot.badword.service.BadwordServiceImpl;
import com.comprehensive.eureka.chatbot.chatroom.entity.ChatRoom;
import com.comprehensive.eureka.chatbot.chatroom.repository.ChatRoomRepository;
import com.comprehensive.eureka.chatbot.langchain.dto.ChatResponseDto;
import com.comprehensive.eureka.chatbot.langchain.repository.ChatMessageRepository;
import com.comprehensive.eureka.chatbot.langchain.session.ChatRoomLockService;
import com.comprehensive.eureka.chatbot.langchain.session.ChatSessionStateService;
import com.comprehensive.eureka.chatbot.langchain.service.ChatService;
import com.comprehensive.eureka.chatbot.langchain.service.impl.ChatMemoryHandler;
import com.comprehensive.eureka.chatbot.langchain.service.impl.SentimentAnalysisService;
import com.comprehensive.eureka.chatbot.sentiment.service.PromptServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat_transaction_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=USER,TIMESTAMP;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=1",
        "openai.api.key=test-key",
        "gemini.api.key=test-key",
        "logging.level.com.comprehensive.eureka.chatbot=WARN"
})
class ChatServiceTransactionBoundaryTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MockBean
    private BadwordServiceImpl badwordService;

    @MockBean
    private SentimentAnalysisService sentimentAnalysisService;

    @MockBean
    private OpenAiChatModel openAiChatModel;

    @MockBean
    private PromptServiceImpl promptService;

    @MockBean
    private ChatRoomLockService chatRoomLockService;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private ChatMemoryHandler chatMemoryHandler;

    @MockBean
    private ChatSessionStateService chatSessionStateService;

    private Long chatRoomId;

    @BeforeEach
    void setUp() throws Exception {
        chatMessageRepository.deleteAll();
        chatRoomRepository.deleteAll();
        ChatRoom chatRoom = chatRoomRepository.save(new ChatRoom(null, 100L, LocalDateTime.now()));
        chatRoomId = chatRoom.getChatRoomId();
        given(badwordService.checkBadWord(any())).willReturn(false);
        given(openAiChatModel.chat(anyList())).willReturn(ChatResponse.builder()
                .aiMessage(AiMessage.from("일반 응답"))
                .build());
        given(chatRoomLockService.execute(any(), any())).willAnswer(invocation -> {
            ChatRoomLockService.CheckedSupplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        given(chatMemoryHandler.getMemoryOfChatRoom(any()))
                .willReturn(MessageWindowChatMemory.withMaxMessages(10));
        given(chatSessionStateService.isPromptProcessing(any())).willReturn(true);
        given(chatSessionStateService.isFirstMessage(any())).willReturn(false);
    }

    @Test
    void calls_sentiment_analysis_without_holding_a_database_transaction() throws Exception {
        AtomicReference<Boolean> transactionActiveAtExternalCall = new AtomicReference<>();
        given(sentimentAnalysisService.analysisSentiment(any())).willAnswer(invocation -> {
            transactionActiveAtExternalCall.set(TransactionSynchronizationManager.isActualTransactionActive());
            return "중립";
        });

        ChatResponseDto response = chatService.generateReply(100L, chatRoomId, "일반 대화");

        assertThat(response.getMessage()).isEqualTo("일반 응답");
        assertThat(transactionActiveAtExternalCall).hasValue(false);
    }

    @Test
    void keeps_the_user_message_when_the_llm_call_fails() throws Exception {
        given(sentimentAnalysisService.analysisSentiment(any())).willReturn("중립");
        given(openAiChatModel.chat(anyList())).willThrow(new IllegalStateException("LLM timeout"));

        assertThatThrownBy(() -> chatService.generateReply(100L, chatRoomId, "응답 실패에도 남아야 하는 사용자 메시지"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅 응답 처리 중 오류가 발생했습니다.")
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("LLM timeout");

        assertThat(chatMessageRepository.findAll())
                .singleElement()
                .satisfies(message -> {
                    assertThat(message.isBot()).isFalse();
                    assertThat(message.getMessage()).isEqualTo("응답 실패에도 남아야 하는 사용자 메시지");
                });
    }
}
