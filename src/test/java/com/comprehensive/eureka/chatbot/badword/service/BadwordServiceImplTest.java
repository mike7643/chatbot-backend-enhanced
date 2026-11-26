package com.comprehensive.eureka.chatbot.badword.service;

import com.comprehensive.eureka.chatbot.badword.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.chatbot.badword.redis.service.AllowWordRedisService;
import com.comprehensive.eureka.chatbot.badword.redis.service.ForbiddenWordRedisService;
import com.comprehensive.eureka.chatbot.client.AdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BadwordServiceImplTest {

    @Mock private ForbiddenWordRedisService forbiddenWordRedisService;
    @Mock private AllowWordRedisService allowWordRedisService;
    @Mock private AdminClient adminClient;

    @InjectMocks private BadwordServiceImpl badwordService;

    @Test
    void 같은_메시지의_기록은_재전송되어도_같은_eventId를_갖는다() {
        given(forbiddenWordRedisService.getAllForbiddenWords()).willReturn(Set.of("바보"));
        given(allowWordRedisService.getAllAllowWords()).willReturn(Set.of());

        badwordService.sendBadwordRecord(1L, 42L, 1_700_000_000L, "너 바보야");
        badwordService.sendBadwordRecord(1L, 42L, 1_700_000_000L, "너 바보야");

        ArgumentCaptor<UserForbiddenWordsChatCreateRequestDto> captor =
                ArgumentCaptor.forClass(UserForbiddenWordsChatCreateRequestDto.class);
        verify(adminClient, times(2)).insertForbiddenWordRecord(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(UserForbiddenWordsChatCreateRequestDto::getEventId)
                .containsOnly("chat-message:42");
        assertThat(captor.getValue().getForbiddenWords()).containsExactly("바보");
    }
}
