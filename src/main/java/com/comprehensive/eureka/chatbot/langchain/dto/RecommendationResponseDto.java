package com.comprehensive.eureka.chatbot.langchain.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResponseDto {
    private UserPreferenceDto userPreference;
    private Double avgDataUsage;
    List<RecommendPlanDto> recommendPlans;
}
