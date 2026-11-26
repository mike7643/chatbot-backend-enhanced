package com.comprehensive.eureka.chatbot.badword.service;

import com.comprehensive.eureka.chatbot.badword.redis.service.AllowWordRedisService;
import com.comprehensive.eureka.chatbot.badword.redis.service.ForbiddenWordRedisService;
import com.comprehensive.eureka.chatbot.langchain.service.impl.BanWordValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 금칙어 필터(Aho-Corasick)를 로컬에 캐시한다.
 *
 * 기존에는 메시지마다 Redis에서 금칙어·허용어 Set 전체를 읽고 필터를 새로 만들었다.
 * 관리자 모듈이 Set을 바꿀 때마다 올리는 버전 키(badword:version)만 메시지마다 확인하고,
 * 버전이 바뀌었거나 최대 보관 시간이 지난 경우에만 다시 만든다.
 */
@Slf4j
@Component
public class BanWordValidatorProvider {

    static final String VERSION_KEY = "badword:version";
    private static final String NO_VERSION = "0";
    /** 버전 키가 유실되거나 증가가 누락돼도 이 시간 안에는 최신 Set으로 다시 만든다. */
    private static final Duration MAX_AGE = Duration.ofSeconds(60);

    private final ForbiddenWordRedisService forbiddenWordRedisService;
    private final AllowWordRedisService allowWordRedisService;
    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    private volatile Snapshot snapshot;

    @Autowired
    public BanWordValidatorProvider(ForbiddenWordRedisService forbiddenWordRedisService,
                                    AllowWordRedisService allowWordRedisService,
                                    StringRedisTemplate redisTemplate) {
        this(forbiddenWordRedisService, allowWordRedisService, redisTemplate, Clock.systemUTC());
    }

    BanWordValidatorProvider(ForbiddenWordRedisService forbiddenWordRedisService,
                             AllowWordRedisService allowWordRedisService,
                             StringRedisTemplate redisTemplate,
                             Clock clock) {
        this.forbiddenWordRedisService = forbiddenWordRedisService;
        this.allowWordRedisService = allowWordRedisService;
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public BanWordValidator get() {
        String version = Objects.requireNonNullElse(redisTemplate.opsForValue().get(VERSION_KEY), NO_VERSION);
        Snapshot current = snapshot;
        if (isFresh(current, version)) {
            return current.validator();
        }
        synchronized (this) {
            current = snapshot;
            if (isFresh(current, version)) {
                return current.validator();
            }
            BanWordValidator validator = new BanWordValidator(
                    copyOf(forbiddenWordRedisService.getAllForbiddenWords()),
                    copyOf(allowWordRedisService.getAllAllowWords()));
            snapshot = new Snapshot(version, clock.instant(), validator);
            log.info("금칙어 필터를 다시 만들었습니다. version={}", version);
            return validator;
        }
    }

    private boolean isFresh(Snapshot current, String version) {
        return current != null
                && current.version().equals(version)
                && current.loadedAt().plus(MAX_AGE).isAfter(clock.instant());
    }

    private static Set<String> copyOf(Set<String> words) {
        return words == null ? Set.of() : new HashSet<>(words);
    }

    private record Snapshot(String version, Instant loadedAt, BanWordValidator validator) {
    }
}
