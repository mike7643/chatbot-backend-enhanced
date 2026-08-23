package com.comprehensive.eureka.chatbot.langchain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {

    private Integer preferenceDataUsage;
    private String preferenceDataUsageUnit;
    private Integer preferenceSharedDataUsage;
    private String preferenceSharedDataUsageUnit;
    private Integer preferencePrice;
    private Long preferenceBenefitGroupId;
    private boolean isPreferenceFamilyData;
    private Integer preferenceValueAddedCallUsage;
}
