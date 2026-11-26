package com.comprehensive.eureka.chatbot.common.exception;

import com.comprehensive.eureka.chatbot.common.dto.BaseResponseDto;
import com.comprehensive.eureka.chatbot.common.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     *  PromptException 처리
     *  ErrorCode에 담긴 코드/메시지를 BaseResponseDto.fail() 로 감싸서 반환
     */
    @ExceptionHandler(PromptException.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handlePromptException(PromptException ex) {
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity
                .badRequest()
                .body(BaseResponseDto.fail(ec));
    }
    /**
    * ChatBotException 처리
    * ErrorCode에 담긴 코드/메시지를 BaseResponseDto.fail() 로 감싸서 반환
     */
    @ExceptionHandler(ChatBotException.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleChatBotException(ChatBotException ex){
        ErrorCode ec = ex.getErrorCode();
        return ResponseEntity
                .badRequest()
                .body(BaseResponseDto.fail(ec));
    }
    /**
     * 같은 채팅방 메시지 처리 중 → 409 Conflict
     */
    @ExceptionHandler(ChatRoomBusyException.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleChatRoomBusyException(ChatRoomBusyException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(BaseResponseDto.fail(ex.getErrorCode()));
    }
    /**
     * 그 외 예기치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponseDto<ErrorResponseDto>> handleException(Exception ex) {
        log.error("Global Exception Handler 예외 발생 - message: {}", ex.getMessage(), ex);
        ErrorCode ec = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(500)
                .body(BaseResponseDto.fail(ec));
    }
}
