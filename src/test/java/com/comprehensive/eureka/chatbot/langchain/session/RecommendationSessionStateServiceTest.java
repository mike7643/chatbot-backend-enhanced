package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.langchain.dto.PlanDto;
import com.comprehensive.eureka.chatbot.langchain.dto.RecommendPlanDto;
import com.comprehensive.eureka.chatbot.langchain.dto.RecommendationResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationSessionStateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RecommendationSessionStateService service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new RecommendationSessionStateService(redisTemplate, objectMapper);
    }

    @Test
    void storesRecommendationContextByChatRoomWithSessionTtl() throws Exception {
        RecommendationSessionContext context = keywordContext();

        service.save(101L, context);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq("chatbot:room:101:recommendation"),
                jsonCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(24L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.HOURS)
        );
        RecommendationSessionContext stored = objectMapper.readValue(jsonCaptor.getValue(), RecommendationSessionContext.class);
        assertThat(stored.getSource()).isEqualTo(RecommendationSource.KEYWORD);
        assertThat(stored.getExtractedKeyword()).isEqualTo("데이터 무제한");
        assertThat(stored.getRecommendPlans()).extracting(plan -> plan.getPlan().getPlanName())
                .containsExactly("5G 프리미어 플러스");
    }

    @Test
    void restoresOnlyTheRecommendationContextForTheRequestedChatRoom() throws Exception {
        String room101Json = objectMapper.writeValueAsString(keywordContext());
        when(valueOperations.get("chatbot:room:101:recommendation")).thenReturn(room101Json);
        when(valueOperations.get("chatbot:room:202:recommendation")).thenReturn(null);

        RecommendationSessionContext room101 = service.find(101L).orElseThrow();

        assertThat(room101.getRecommendPlans().get(0).getPlan().getPlanId()).isEqualTo(1L);
        assertThat(service.find(202L)).isEmpty();
    }

    @Test
    void restoresPreferenceBasedRecommendationResponseForFeedback() throws Exception {
        RecommendPlanDto recommendPlan = RecommendPlanDto.builder()
                .plan(PlanDto.builder().planId(2L).planName("5G 시그니처").build())
                .build();
        RecommendationResponseDto response = RecommendationResponseDto.builder()
                .recommendPlans(List.of(recommendPlan))
                .build();
        when(valueOperations.get("chatbot:room:101:recommendation"))
                .thenReturn(objectMapper.writeValueAsString(RecommendationSessionContext.preference(response)));

        RecommendationSessionContext restored = service.find(101L).orElseThrow();

        assertThat(restored.getSource()).isEqualTo(RecommendationSource.PREFERENCE);
        assertThat(restored.getRecommendationResponse().getRecommendPlans().get(0).getPlan().getPlanId()).isEqualTo(2L);
        assertThat(restored.getRecommendPlans()).hasSize(1);
    }

    @Test
    void removesRecommendationContextWhenTheConversationEnds() {
        service.clear(101L);

        verify(redisTemplate).delete("chatbot:room:101:recommendation");
    }

    private RecommendationSessionContext keywordContext() {
        RecommendPlanDto recommendPlan = RecommendPlanDto.builder()
                .plan(PlanDto.builder().planId(1L).planName("5G 프리미어 플러스").build())
                .recommendationType("KEYWORD")
                .build();
        return RecommendationSessionContext.keyword("데이터 무제한", List.of(recommendPlan));
    }
}
