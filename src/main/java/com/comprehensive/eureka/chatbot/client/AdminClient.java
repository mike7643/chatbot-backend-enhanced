package com.comprehensive.eureka.chatbot.client;

import com.comprehensive.eureka.chatbot.badword.dto.request.UserForbiddenWordsChatCreateRequestDto;
import com.comprehensive.eureka.chatbot.common.dto.BaseResponseDto;
import com.comprehensive.eureka.chatbot.constant.DomainConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
@Slf4j
public class AdminClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);

    private final WebClient webClient;
    private final String adminDomain;

    @Autowired
    public AdminClient(WebClient webClient) {
        this(webClient, DomainConstant.ADMIN_DOMAIN);
    }

    AdminClient(WebClient webClient, String adminDomain) {
        this.webClient = webClient;
        this.adminDomain = adminDomain;
    }

    /**
     * 금칙어 사용 기록 전송.
     * admin이 eventId로 중복 처리를 막기 때문에, 연결 실패·5xx 같은 일시적 오류는 재시도해도 이중 집계되지 않는다.
     * 4xx는 요청 자체의 문제이므로 재시도하지 않는다.
     */
    public BaseResponseDto<Object> insertForbiddenWordRecord(UserForbiddenWordsChatCreateRequestDto request) {
        return webClient.post()
                .uri(adminDomain + "/admin/forbidden-words/chats")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<BaseResponseDto<Object>>() {})
                .timeout(TIMEOUT)
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .filter(AdminClient::isTransient)
                        .doBeforeRetry(signal -> log.warn("금칙어 기록 전송 재시도: eventId={}, attempt={}",
                                request.getEventId(), signal.totalRetries() + 1, signal.failure()))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .block();
    }

    static boolean isTransient(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return throwable instanceof WebClientRequestException
                || throwable instanceof java.util.concurrent.TimeoutException;
    }
}
