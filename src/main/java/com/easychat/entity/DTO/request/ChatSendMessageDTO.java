package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class ChatSendMessageDTO {
    private Integer contactId;
    private String messageContent;
    private Integer messageType;
    private Long fileSize;
    private String filename;
    private Integer fileType;
}
