package com.comprehensive.eureka.chatbot.langchain.session;

import com.comprehensive.eureka.chatbot.common.exception.ChatRoomBusyException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ChatRoomLockService {

    private static final long WAIT_SECONDS = 2;
    private final RedissonClient redissonClient;

    public <T> T execute(Long chatRoomId, CheckedSupplier<T> action) throws Exception {
        RLock lock = redissonClient.getLock("chatbot:room:" + chatRoomId + ":lock");
        try {
            // leaseTime을 지정하지 않아 Redisson watchdog이 LLM 응답 대기 동안 락을 연장한다.
            if (!lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new ChatRoomBusyException();
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("채팅방 처리 순서를 확보하지 못했습니다.", exception);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}
