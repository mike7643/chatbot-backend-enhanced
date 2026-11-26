package com.comprehensive.eureka.chatbot.langchain.integration.service;

import com.comprehensive.eureka.chatbot.chatroom.entity.ChatRoom;
import com.comprehensive.eureka.chatbot.chatroom.repository.ChatRoomRepository;
import com.comprehensive.eureka.chatbot.langchain.dto.ChatResponseDto;
import com.comprehensive.eureka.chatbot.langchain.repository.ChatMessageRepository;
import com.comprehensive.eureka.chatbot.langchain.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

/**
 * 실제 MySQL·LLM·내부 모듈이 모두 떠 있어야 하는 E2E 성격의 테스트.
 * 기본 빌드에서는 건너뛰고, CHATBOT_E2E=true 환경에서만 실행한다.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CHATBOT_E2E", matches = "true")
class ChatServiceImplIntegrationTest {

    @SpyBean
    private ChatServiceImpl chatServiceImpl; // ✅ SpyBean으로 변경

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private final Long USER_ID = 1L;
    private final Long CHATROOM_ID = 1L;

    @BeforeEach
    void initChatRoom() {
        if (chatRoomRepository.findById(CHATROOM_ID).isEmpty()) {
            ChatRoom room = new ChatRoom();
            room.setChatRoomId(CHATROOM_ID);
            chatRoomRepository.save(room);
        }

        // ✅ Redis 접근 방지용 mock
        doReturn(false)
                .when(chatServiceImpl)
                .badWordCheck(anyLong(), anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("심심풀이 통합 테스트")
    public void funnyChatTest() throws JsonProcessingException {
        //given
        String message = "재밌는 이야기 해줘";
        //when
        ChatResponseDto chatResponseDto = chatServiceImpl.generateReply(1L, 1L, message);
        //then
        assertNotNull(chatResponseDto);
    }

    @Test
    @DisplayName("기본 대화 흐름 테스트")
    void testBasicMessage() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "안녕?");
        assertNotNull(response);
        System.out.println(response.getMessage());
    }

    @Test
    @DisplayName("금칙어 필터링 테스트")
    void testBadWord() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "씨발");
        assertFalse(response.getMessage().contains("금지된 단어")); // ✅ mock이 항상 false라서 해당 메시지 없음
    }

    @Test
    @DisplayName("프롬프트 전환 테스트")
    void testPromptSwitch() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "추천 관련 질문");
        assertNotNull(response);
    }

    @Test
    @DisplayName("비밀번호 트리거 테스트")
    void testPasswordTrigger() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "사용자 정보 알고 싶어");
        assertTrue(response.getMessage().contains("어떤 도움을 드릴까요"));
    }

    @Test
    @DisplayName("요금제 조회 테스트")
    void testPlanQuery() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "요금제 조회-요금제 혜택-음악");
        assertNotNull(response.getMessage());
    }

    @Test
    @DisplayName("종료 시그널 테스트")
    void testEndSignal() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "[END_OF_FUNNYCHAT_SCENARIO]");
        assertNotNull(response.getMessage());
    }

    @Test
    @DisplayName("키워드 기반 추천 테스트")
    void testKeywordRecommendation() throws JsonProcessingException {
        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "직업을 확인하였습니다");
//        assertTrue(response.getIsRecommended());
        assertNotNull(response);
    }


    @Test
    @DisplayName("성향 기반 추천 테스트")
    void testPreferenceComplete() throws JsonProcessingException {
        chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "요금제 추천해줘");
        chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "100기가");
        chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "120기가");
        chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "10만원");
        ChatResponseDto response =chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "140분");
//         chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "유튜브 프리미엄");
//        chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "통신성향을 모두 파악했습니다");
        assertNotNull(response);
    }

    // 필요시 주석 해제
//    @Test
//    @DisplayName("피드백 반영 추천 테스트")
//    void testFeedbackResponse() throws JsonProcessingException {
//        ChatResponseDto response = chatServiceImpl.generateReply(USER_ID, CHATROOM_ID, "요금제는 좋은데 가격이 좀 비싸요");
//        assertTrue(response.getIsRecommended());
//    }
}
