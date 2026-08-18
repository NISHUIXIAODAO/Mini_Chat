package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserPresenceServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private UserPresenceService service;

    @BeforeEach
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        ClusterNodeProperties properties = new ClusterNodeProperties();
        properties.setNodeId("node-a");
        service = new UserPresenceService(redisTemplate, properties);
    }

    @Test
    public void connectWritesNodeAndConnectionHashes() {
        service.connect(1001, "connection-a");
        verify(hashOperations).putAll(eq("ws:user:active:1001"), anyMap());
        verify(hashOperations).putAll(eq("ws:connection:connection-a"), anyMap());
    }

    @Test
    public void disconnectOldConnectionDoesNotDeleteNewActivePresence() {
        Map<Object, Object> active = new HashMap<>();
        active.put("nodeId", "node-a");
        active.put("connectionId", "connection-new");
        when(hashOperations.entries("ws:user:active:1001")).thenReturn(active);
        service.disconnect(1001, "connection-old");
        verify(redisTemplate, never()).delete("ws:user:active:1001");
        verify(redisTemplate).delete("ws:connection:connection-old");
    }
}
