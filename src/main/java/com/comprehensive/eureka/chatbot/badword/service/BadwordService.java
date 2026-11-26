package com.comprehensive.eureka.chatbot.badword.service;

import com.comprehensive.eureka.chatbot.badword.dto.request.BadwordRequest;
import com.comprehensive.eureka.chatbot.badword.dto.response.BadwordResponse;
import com.comprehensive.eureka.chatbot.badword.dto.response.ForbiddenWordResponseDto;
import com.comprehensive.eureka.chatbot.common.dto.BaseResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;


public interface BadwordService {

//    void createBadWord(BadwordRequest badwordRequest);

    Set<String> getAllForbiddendWords();
    Set<String> getAllAllowedWords();
    boolean checkBadWord(String message) throws JsonProcessingException;
    void sendBadwordRecord(Long userId, Long chatMessageId, Long sentAt, String message);
//    void deleteBadWord(String word);
}
