package com.comprehensive.eureka.chatbot.client;

import com.comprehensive.eureka.chatbot.badword.dto.request.UserForbiddenWordsChatCreateRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminClientTest {

    private static final String OK_BODY = "{\"statusCode\":200,\"message\":\"success\",\"data\":null}";

    private final UserForbiddenWordsChatCreateRequestDto request = UserForbiddenWordsChatCreateRequestDto.builder()
            .userId(1L)
            .chatMessageText("메시지")
            .sentAt(1L)
            .forbiddenWords(List.of("욕설"))
            .eventId("chat-message:1")
            .build();

    @Test
    void admin_일시_장애_5xx는_재시도해서_성공한다() {
        AtomicInteger calls = new AtomicInteger();
        AdminClient client = clientResponding(calls, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.OK);

        client.insertForbiddenWordRecord(request);

        assertThat(calls).hasValue(3);
    }

    @Test
    void 요청_오류_4xx는_재시도하지_않는다() {
        AtomicInteger calls = new AtomicInteger();
        AdminClient client = clientResponding(calls, HttpStatus.BAD_REQUEST);

        assertThatThrownBy(() -> client.insertForbiddenWordRecord(request))
                .isInstanceOf(WebClientResponseException.BadRequest.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void 재시도_횟수를_넘기면_마지막_오류를_그대로_던진다() {
        AtomicInteger calls = new AtomicInteger();
        AdminClient client = clientResponding(calls, HttpStatus.SERVICE_UNAVAILABLE);

        assertThatThrownBy(() -> client.insertForbiddenWordRecord(request))
                .isInstanceOf(WebClientResponseException.ServiceUnavailable.class);
        assertThat(calls).hasValue(3);
    }

    /** 호출 순서대로 statuses를 응답하고, 마지막 상태는 이후 호출에도 반복한다. */
    private AdminClient clientResponding(AtomicInteger calls, HttpStatus... statuses) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(clientRequest -> {
                    int index = Math.min(calls.getAndIncrement(), statuses.length - 1);
                    HttpStatus status = statuses[index];
                    return Mono.just(ClientResponse.create(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(status.is2xxSuccessful() ? OK_BODY : "{}")
                            .build());
                })
                .build();
        return new AdminClient(webClient, "http://admin.test");
    }
}
