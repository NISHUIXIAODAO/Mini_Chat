package com.easychat.service.application;

import com.alibaba.fastjson.JSON;
import com.easychat.entity.DO.MessageOutbox;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.event.PushEvent;
import com.easychat.enums.OutboxStatusEnum;
import com.easychat.mapper.MessageOutboxMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MessageOutboxService {
    public static final String CHAT_MESSAGE_EVENT = "CHAT_MESSAGE";
    private final MessageOutboxMapper messageOutboxMapper;

    public MessageOutboxService(MessageOutboxMapper messageOutboxMapper) {
        this.messageOutboxMapper = messageOutboxMapper;
    }

    public PushEvent createChatMessage(Long messageId, String sessionId, List<Integer> targetUserIds,
                                       MessageSendDTO<?> messagePayload) {
        long now = System.currentTimeMillis();
        PushEvent event = new PushEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTraceId(UUID.randomUUID().toString());
        event.setEventType(CHAT_MESSAGE_EVENT);
        event.setMessageId(messageId);
        event.setSessionId(sessionId);
        event.setTargetUserIds(targetUserIds);
        event.setMessagePayload(messagePayload);
        event.setCreatedAt(now);

        MessageOutbox outbox = new MessageOutbox()
                .setEventId(event.getEventId())
                .setAggregateId(messageId)
                .setEventType(CHAT_MESSAGE_EVENT)
                .setPayload(JSON.toJSONString(event))
                .setStatus(OutboxStatusEnum.PENDING.name())
                .setRetryCount(0)
                .setNextRetryAt(now)
                .setTraceId(event.getTraceId())
                .setCreateTime(now)
                .setUpdateTime(now);
        messageOutboxMapper.insert(outbox);
        return event;
    }

    public boolean retryFailed(Long outboxId) {
        return outboxId != null && messageOutboxMapper.retryFailed(outboxId, System.currentTimeMillis()) > 0;
    }
}
