package com.comprehensive.eureka.chatbot.badword.service;

import com.comprehensive.eureka.chatbot.badword.redis.service.AllowWordRedisService;
import com.comprehensive.eureka.chatbot.badword.redis.service.ForbiddenWordRedisService;
import com.comprehensive.eureka.chatbot.langchain.service.impl.BanWordValidator;
import com.comprehensive.eureka.chatbot.support.RedisContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BanWordValidatorProviderTest extends RedisContainerSupport {

    private final StringRedisTemplate redisTemplate = newRedisTemplate();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-27T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private BanWordValidatorProvider provider;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(java.util.List.of("forbidden:words", "allow:words", BanWordValidatorProvider.VERSION_KEY));
        redisTemplate.opsForSet().add("forbidden:words", "바보");
        redisTemplate.opsForValue().set(BanWordValidatorProvider.VERSION_KEY, "1");
        provider = new BanWordValidatorProvider(
                new ForbiddenWordRedisService(redisTemplate), new AllowWordRedisService(redisTemplate), redisTemplate, clock);
    }

    @Test
    void 버전이_같으면_필터를_다시_만들지_않는다() {
        BanWordValidator first = provider.get();

        redisTemplate.opsForSet().add("forbidden:words", "멍청이");
        BanWordValidator second = provider.get();

        assertThat(second).isSameAs(first);
        assertThat(second.checkBanWord("멍청이")).isFalse();
    }

    @Test
    void 관리자가_버전을_올리면_새_금칙어로_다시_만든다() {
        BanWordValidator first = provider.get();

        redisTemplate.opsForSet().add("forbidden:words", "멍청이");
        redisTemplate.opsForValue().increment(BanWordValidatorProvider.VERSION_KEY);
        BanWordValidator second = provider.get();

        assertThat(second).isNotSameAs(first);
        assertThat(second.checkBanWord("너 멍청이야")).isTrue();
    }

    @Test
    void 버전_증가가_누락돼도_최대_보관_시간이_지나면_다시_만든다() {
        BanWordValidator first = provider.get();
        redisTemplate.opsForSet().add("forbidden:words", "멍청이");

        now.set(now.get().plus(Duration.ofSeconds(61)));
        BanWordValidator second = provider.get();

        assertThat(second).isNotSameAs(first);
        assertThat(second.checkBanWord("멍청이")).isTrue();
    }

    @Test
    void 허용어에_포함된_금칙어는_걸러내지_않는다() {
        redisTemplate.opsForSet().add("forbidden:words", "시발");
        redisTemplate.opsForSet().add("allow:words", "시발점");
        redisTemplate.opsForValue().increment(BanWordValidatorProvider.VERSION_KEY);

        assertThat(provider.get().checkBanWord("모든 일의 시발점")).isFalse();
        assertThat(provider.get().findBanWords("시발 진짜")).containsExactly("시발");
    }
}
