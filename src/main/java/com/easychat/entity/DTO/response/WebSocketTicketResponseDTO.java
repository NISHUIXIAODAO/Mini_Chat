package com.easychat.entity.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebSocketTicketResponseDTO {
    private String ticket;
    private long expiresInMillis;
}
