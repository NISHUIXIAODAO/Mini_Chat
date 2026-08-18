package com.easychat.entity.event;

import com.easychat.entity.DTO.request.MessageSendDTO;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class PushEvent implements Serializable {
    private String eventId;
    private String traceId;
    private String eventType;
    private Long messageId;
    private String sessionId;
    private List<Integer> targetUserIds;
    private MessageSendDTO<?> messagePayload;
    private Long createdAt;
}
