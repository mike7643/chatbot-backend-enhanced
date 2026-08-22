package com.comprehensive.eureka.chatbot.langchain.session;

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
            if (!lock.tryLock(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동일 채팅방의 이전 메시지를 처리 중입니다.");
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
