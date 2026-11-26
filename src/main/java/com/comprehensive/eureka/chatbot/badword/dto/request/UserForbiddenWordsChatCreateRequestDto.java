package com.comprehensive.eureka.chatbot.badword.dto.request;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserForbiddenWordsChatCreateRequestDto {
    private Long userId;
    private String chatMessageText;
    private Long sentAt;
    private List<String> forbiddenWords;
    /** 같은 메시지의 재전송을 admin이 중복 집계하지 않도록 메시지 ID 기반으로 생성한다. */
    private String eventId;
}
