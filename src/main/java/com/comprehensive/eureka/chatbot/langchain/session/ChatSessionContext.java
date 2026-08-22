package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.langchain.dto.RecommendPlanDto;
import com.comprehensive.eureka.chatbot.langchain.dto.RecommendationResponseDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatSessionContext {
    private RecommendationResponseDto recommendationResponse;
    private List<RecommendPlanDto> recommendPlans;
    private String extractedKeyword;
}
