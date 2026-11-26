package com.comprehensive.eureka.chatbot.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(20000, "INTERNAL_SERVER_ERROR",  "서버 내부 오류가 발생했습니다."),
    //Domain error
    DOMAIN_NOT_CHANGED(20034,"DOMAIN_NOT_CHANGED","domain주소를 안바꾸셨습니다"),
    // 프롬프트 관리 에러 (20010~20019)
    PROMPT_NOT_FOUND       (20010, "PROMPT_NOT_FOUND", "해당 프롬프트를 찾을 수 없습니다."),
    SENTIMENT_CODE_ALREADY_EXISTS  (20011, "SENTIMENT_CODE_ALREADY_EXISTS", "이미 등록된 감정코드입니다."),
    SENTIMENT_NAME_ALREADY_EXISTS  (20011, "SENTIMENT_NAME_ALREADY_EXISTS", "이미 등록된 감정입니다."),
    PROMPT_CREATE_FAILED   (20012, "PROMPT_CREATE_FAILED", "프롬프트 등록에 실패했습니다."),
    PROMPT_UPDATE_FAILED   (20013, "PROMPT_UPDATE_FAILED", "프롬프트 수정에 실패했습니다."),
    PROMPT_DELETE_FAILED   (20014, "PROMPT_DELETE_FAILED", "프롬프트 삭제에 실패했습니다."),

    // 금칙어 관리 에러 (20010~20019)
    FORBIDDEN_WORD_NOT_FOUND       (20020, "FORBIDDEN_WORD_NOT_FOUND",       "해당 금칙어를 찾을 수 없습니다."),
    FORBIDDEN_WORD_ALREADY_EXISTS  (20021, "FORBIDDEN_WORD_ALREADY_EXISTS",  "이미 등록된 금칙어입니다."),
    FORBIDDEN_WORD_CREATE_FAILED   (20022, "FORBIDDEN_WORD_CREATE_FAILED",   "금칙어 등록에 실패했습니다."),
    FORBIDDEN_WORD_UPDATE_FAILED   (20023, "FORBIDDEN_WORD_UPDATE_FAILED",   "금칙어 수정에 실패했습니다."),
    FORBIDDEN_WORD_DELETE_FAILED   (20024, "FORBIDDEN_WORD_DELETE_FAILED",   "금칙어 삭제에 실패했습니다."),
    FORBIDDEN_WORD_TOGGLE_FAILED   (20025, "FORBIDDEN_WORD_TOGGLE_FAILED",   "금칙어 사용 여부 전환에 실패했습니다."),

    // 금칙어 로그 관리 에러 (20020~20029)
    FORBIDDEN_WORD_LOG_NOT_FOUND       (20030, "FORBIDDEN_WORD_LOG_NOT_FOUND",       "해당 금칙어 로그를 찾을 수 없습니다."),
    FORBIDDEN_WORD_LOG_RETRIEVE_FAILED (20031, "FORBIDDEN_WORD_LOG_RETRIEVE_FAILED", "금칙어 로그 조회에 실패했습니다."),
    FORBIDDEN_WORD_LOG_DELETE_FAILED   (20032, "FORBIDDEN_WORD_LOG_DELETE_FAILED",   "금칙어 로그 삭제에 실패했습니다."),

    // 채팅 메시지 조회 에러
    CHAT_MESSAGE_RETRIEVE_FAILED      (20033, "CHAT_MESSAGE_RETRIEVE_FAILED",      "채팅 메시지를 조회할 수 없습니다."),
    //챗봇 답변 생성 에러
    CHATBOT_PROMPT_ERROR(20034,"CHATBOT_PROMPT_ERROR","챗봇 답변을 생성 중 에러 발생했습니다."),
    CHAT_ROOM_BUSY(20035, "CHAT_ROOM_BUSY", "이전 메시지에 대한 답변을 준비 중입니다. 잠시 후 다시 보내주세요."),
    //외부 api호출 오류
    DATA_NOT_FOUND(20035, "DATA_NOT_FOUND","외부 API 호출에서 실패했습니다.");
    private final int code;
    private final String name;
    private final String message;
}
