package com.comprehensive.eureka.chatbot.langchain.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendPlanDto {
    private PlanDto plan;
    private Double score;
    private String recommendationType;
    private List<BenefitDto> benefits;
}
