package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.langchain.dto.RecommendPlanDto;
import com.comprehensive.eureka.chatbot.langchain.dto.RecommendationResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSessionContext {

    private RecommendationResponseDto recommendationResponse;
    private List<RecommendPlanDto> recommendPlans;
    private String extractedKeyword;
    private RecommendationSource source;

    public static RecommendationSessionContext preference(RecommendationResponseDto recommendationResponse) {
        return new RecommendationSessionContext(
                recommendationResponse,
                recommendationResponse.getRecommendPlans(),
                null,
                RecommendationSource.PREFERENCE
        );
    }

    public static RecommendationSessionContext keyword(String extractedKeyword, List<RecommendPlanDto> recommendPlans) {
        return new RecommendationSessionContext(null, recommendPlans, extractedKeyword, RecommendationSource.KEYWORD);
    }

    public RecommendationSessionContext withFeedbackRecommendation(RecommendationResponseDto recommendationResponse) {
        return new RecommendationSessionContext(
                recommendationResponse,
                recommendationResponse.getRecommendPlans(),
                extractedKeyword,
                source
        );
    }
}
