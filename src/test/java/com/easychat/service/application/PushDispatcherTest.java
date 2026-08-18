package com.easychat.service.application;

import com.alibaba.fastjson.JSON;
import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.cluster.UserPresence;
import com.easychat.entity.event.PushEvent;
import com.easychat.service.cluster.UserPresenceService;
import com.easychat.webSocket.LocalChannelRegistry;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushDispatcherTest {
    @Test
    public void dispatchOnlyPushesUsersPresentOnCurrentNode() {
        UserPresenceService presenceService = mock(UserPresenceService.class);
        LocalChannelRegistry registry = mock(LocalChannelRegistry.class);
        ClusterNodeProperties properties = new ClusterNodeProperties();
        properties.setNodeId("node-a");
        when(presenceService.isPushEventConsumed("event-1")).thenReturn(false);
        when(presenceService.findActive(2)).thenReturn(new UserPresence(2, "node-a", "c2"));
        when(presenceService.findActive(3)).thenReturn(new UserPresence(3, "node-b", "c3"));
        when(registry.send(any(MessageSendDTO.class), eq(2))).thenReturn(true);

        PushDispatcher dispatcher = new PushDispatcher(presenceService, registry, properties);
        dispatcher.dispatch(JSON.toJSONString(event()));

        verify(registry).send(any(MessageSendDTO.class), eq(2));
        verify(registry, never()).send(any(MessageSendDTO.class), eq(3));
        verify(presenceService).markPushEventConsumed("event-1");
    }

    @Test
    public void duplicateEventDoesNotPushAgain() {
        UserPresenceService presenceService = mock(UserPresenceService.class);
        LocalChannelRegistry registry = mock(LocalChannelRegistry.class);
        ClusterNodeProperties properties = new ClusterNodeProperties();
        properties.setNodeId("node-a");
        when(presenceService.isPushEventConsumed("event-1")).thenReturn(true);

        new PushDispatcher(presenceService, registry, properties).dispatch(JSON.toJSONString(event()));

        verify(registry, never()).send(any(MessageSendDTO.class), any(Integer.class));
        verify(presenceService, never()).markPushEventConsumed("event-1");
    }

    private PushEvent event() {
        PushEvent event = new PushEvent();
        event.setEventId("event-1");
        event.setTraceId("trace-1");
        event.setMessageId(1L);
        event.setTargetUserIds(Arrays.asList(2, 3));
        event.setMessagePayload(new MessageSendDTO<>());
        return event;
    }
}
