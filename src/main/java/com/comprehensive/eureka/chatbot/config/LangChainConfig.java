package com.comprehensive.eureka.chatbot.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import com.comprehensive.eureka.chatbot.langchain.session.RedisChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private static final String GEMINI_OPENAI_COMPATIBLE_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai/";
    private static final String MODEL_NAME = "gemini-3.1-flash-lite";

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(GEMINI_OPENAI_COMPATIBLE_BASE_URL)
                .apiKey(geminiApiKey)
                .modelName(MODEL_NAME)
                .build();
    }

    @Bean
    public ChatMemoryStore memoryStore(RedisChatMemoryStore redisChatMemoryStore) {
        return redisChatMemoryStore;
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        // 대화 메모리의 길이 제한용 근사치로만 OpenAI 토크나이저를 사용한다.
        return new OpenAiTokenCountEstimator("gpt-4.1-mini");
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ChatMemoryStore memoryStore,
                                                 TokenCountEstimator estimator) {
        return userId -> TokenWindowChatMemory.builder()
                .id(userId)
                .maxTokens(1000, estimator)
                .chatMemoryStore(memoryStore)
                .build();
    }
}
