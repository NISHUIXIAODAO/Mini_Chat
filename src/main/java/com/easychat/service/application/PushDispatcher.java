package com.easychat.service.application;

import com.alibaba.fastjson.JSON;
import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.cluster.UserPresence;
import com.easychat.entity.event.PushEvent;
import com.easychat.service.cluster.UserPresenceService;
import com.easychat.webSocket.LocalChannelRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushDispatcher {
    private final UserPresenceService userPresenceService;
    private final LocalChannelRegistry localChannelRegistry;
    private final ClusterNodeProperties clusterProperties;

    public PushDispatcher(UserPresenceService userPresenceService,
                          LocalChannelRegistry localChannelRegistry,
                          ClusterNodeProperties clusterProperties) {
        this.userPresenceService = userPresenceService;
        this.localChannelRegistry = localChannelRegistry;
        this.clusterProperties = clusterProperties;
    }

    public void dispatch(String payload) {
        PushEvent event = JSON.parseObject(payload, PushEvent.class);
        if (event == null || event.getEventId() == null || event.getTargetUserIds() == null) {
            throw new IllegalArgumentException("Invalid push event");
        }
        if (userPresenceService.isPushEventConsumed(event.getEventId())) {
            log.info("push event duplicated traceId={}, eventId={}, messageId={}, nodeId={}",
                    event.getTraceId(), event.getEventId(), event.getMessageId(), clusterProperties.getNodeId());
            return;
        }

        int localMatches = 0;
        int sent = 0;
        for (Integer userId : event.getTargetUserIds()) {
            UserPresence presence = userPresenceService.findActive(userId);
            if (presence == null || !clusterProperties.getNodeId().equals(presence.getNodeId())) {
                continue;
            }
            localMatches++;
            MessageSendDTO<?> message = copyMessage(event.getMessagePayload());
            if (localChannelRegistry.send(message, userId)) {
                sent++;
            } else {
                log.warn("push local channel missing traceId={}, eventId={}, messageId={}, userId={}, nodeId={}",
                        event.getTraceId(), event.getEventId(), event.getMessageId(), userId, clusterProperties.getNodeId());
            }
        }
        log.info("push event dispatched traceId={}, eventId={}, messageId={}, localMatches={}, sent={}, nodeId={}",
                event.getTraceId(), event.getEventId(), event.getMessageId(), localMatches, sent, clusterProperties.getNodeId());
        userPresenceService.markPushEventConsumed(event.getEventId());
    }

    private MessageSendDTO<?> copyMessage(MessageSendDTO<?> message) {
        return JSON.parseObject(JSON.toJSONString(message), MessageSendDTO.class);
    }
}
