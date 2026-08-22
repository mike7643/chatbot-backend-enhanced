package com.comprehensive.eureka.chatbot.langchain.service.impl;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMemoryHandler {
    private final TokenCountEstimator tokenCountEstimator;
    private final ChatMemoryStore memoryStore;
    public ChatMemory getMemoryOfChatRoom(Long chatRoomId){
        return TokenWindowChatMemory.builder()
                .id(chatRoomId)
                .maxTokens(10000, tokenCountEstimator)
                .chatMemoryStore(memoryStore)
                .build();
    }
}
