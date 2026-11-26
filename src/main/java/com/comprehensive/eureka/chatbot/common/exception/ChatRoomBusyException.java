package com.comprehensive.eureka.chatbot.common.exception;

/**
 * 같은 채팅방의 이전 메시지가 아직 처리 중이라 순서를 보장할 수 없을 때 발생한다.
 * 서버 오류가 아니라 클라이언트가 잠시 후 다시 보내면 되는 상황이므로 409로 응답한다.
 */
public class ChatRoomBusyException extends ChatException {

    public ChatRoomBusyException() {
        super(ErrorCode.CHAT_ROOM_BUSY);
    }
}
