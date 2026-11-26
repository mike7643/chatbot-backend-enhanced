package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.common.exception.ChatRoomBusyException;
import com.comprehensive.eureka.chatbot.support.RedisContainerSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 채팅방 단위 분산락: 같은 방은 직렬화하고, 다른 방은 병렬로 처리되는지 검증한다.
 */
class ChatRoomLockServiceTest extends RedisContainerSupport {

    private static RedissonClient redissonClient;
    private static ChatRoomLockService lockService;

    @BeforeAll
    static void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress(redisAddress());
        redissonClient = Redisson.create(config);
        lockService = new ChatRoomLockService(redissonClient);
    }

    @AfterAll
    static void tearDown() {
        redissonClient.shutdown();
    }

    @Test
    void 같은_채팅방의_이전_메시지가_처리_중이면_대기_후_ChatRoomBusyException을_던진다() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = holdLock(1L, holding, release);
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        long startedAt = System.nanoTime();
        assertThatThrownBy(() -> lockService.execute(1L, () -> "두 번째 메시지"))
                .isInstanceOf(ChatRoomBusyException.class);
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)).isGreaterThanOrEqualTo(1_900);

        release.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("첫 번째 메시지");
    }

    @Test
    void 다른_채팅방은_락을_기다리지_않고_바로_처리된다() throws Exception {
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<String> first = holdLock(10L, holding, release);
        assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();

        long startedAt = System.nanoTime();
        String result = lockService.execute(11L, () -> "다른 방 메시지");

        assertThat(result).isEqualTo("다른 방 메시지");
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)).isLessThan(1_000);
        release.countDown();
        first.get(5, TimeUnit.SECONDS);
    }

    @Test
    void 이전_메시지가_끝나면_같은_채팅방의_다음_메시지를_처리한다() throws Exception {
        assertThat(lockService.execute(20L, () -> "첫 번째")).isEqualTo("첫 번째");
        assertThat(lockService.execute(20L, () -> "두 번째")).isEqualTo("두 번째");
    }

    private CompletableFuture<String> holdLock(Long chatRoomId, CountDownLatch holding, CountDownLatch release) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return lockService.execute(chatRoomId, () -> {
                    holding.countDown();
                    release.await(10, TimeUnit.SECONDS);
                    return "첫 번째 메시지";
                });
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
