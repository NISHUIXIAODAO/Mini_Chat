package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.cluster.ClusterNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NodeRegistryServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, Object, Object> hashOperations;
    private NodeRegistryService service;

    @BeforeEach
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        SetOperations<String, Object> setOperations = mock(SetOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        service = new NodeRegistryService(redisTemplate, new ClusterNodeProperties());
    }

    @Test
    public void registerStoresNodeHash() {
        service.register("node-b", "10.0.0.2", 5050, 5051);
        verify(hashOperations).putAll(eq("cluster:node:node-b"), anyMap());
    }

    @Test
    public void findHealthyNodeRequiresUpStatus() {
        Map<Object, Object> values = new HashMap<>();
        values.put("status", "UP");
        values.put("internalHost", "10.0.0.2");
        values.put("internalPort", 5050);
        values.put("wsPort", 5051);
        when(hashOperations.entries("cluster:node:node-b")).thenReturn(values);
        ClusterNode node = service.findHealthyNode("node-b");
        assertEquals("10.0.0.2", node.getInternalHost());
        values.put("status", "DOWN");
        assertNull(service.findHealthyNode("node-b"));
    }
}
