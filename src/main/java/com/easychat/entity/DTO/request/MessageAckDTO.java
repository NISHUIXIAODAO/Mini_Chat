package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class MessageAckDTO {
    private Long messageId;
    private String sessionId;
    private Integer contactId;
}
