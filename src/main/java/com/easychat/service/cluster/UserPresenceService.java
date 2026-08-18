package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.cluster.UserPresence;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserPresenceService {
    private static final String ACTIVE_PREFIX = "ws:user:active:";
    private static final String CONNECTION_PREFIX = "ws:connection:";
    private static final String NONCE_PREFIX = "cluster:nonce:";
    private static final String CONSUMED_EVENT_PREFIX = "push:consumed:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ClusterNodeProperties properties;

    public UserPresenceService(RedisTemplate<String, Object> redisTemplate, ClusterNodeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void connect(Integer userId, String connectionId) {
        if (userId == null || connectionId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> active = new HashMap<>();
        active.put("nodeId", properties.getNodeId());
        active.put("connectionId", connectionId);
        active.put("loginAt", now);
        active.put("lastHeartbeat", now);
        redisTemplate.opsForHash().putAll(activeKey(userId), active);
        redisTemplate.expire(activeKey(userId), properties.getPresenceTtlSeconds(), TimeUnit.SECONDS);

        Map<String, Object> connection = new HashMap<>();
        connection.put("userId", userId);
        connection.put("nodeId", properties.getNodeId());
        connection.put("channelId", connectionId);
        redisTemplate.opsForHash().putAll(connectionKey(connectionId), connection);
        redisTemplate.expire(connectionKey(connectionId), properties.getPresenceTtlSeconds(), TimeUnit.SECONDS);
    }

    public void refresh(Integer userId, String connectionId) {
        if (userId == null || connectionId == null) {
            return;
        }
        UserPresence active = findActive(userId);
        if (active == null || !connectionId.equals(active.getConnectionId())) {
            return;
        }
        redisTemplate.opsForHash().put(activeKey(userId), "lastHeartbeat", System.currentTimeMillis());
        redisTemplate.expire(activeKey(userId), properties.getPresenceTtlSeconds(), TimeUnit.SECONDS);
        redisTemplate.expire(connectionKey(connectionId), properties.getPresenceTtlSeconds(), TimeUnit.SECONDS);
    }

    public void disconnect(Integer userId, String connectionId) {
        if (userId == null || connectionId == null) {
            return;
        }
        UserPresence active = findActive(userId);
        if (active != null && connectionId.equals(active.getConnectionId())) {
            redisTemplate.delete(activeKey(userId));
        }
        redisTemplate.delete(connectionKey(connectionId));
    }

    public UserPresence findActive(Integer userId) {
        if (userId == null) {
            return null;
        }
        Map<Object, Object> values = redisTemplate.opsForHash().entries(activeKey(userId));
        if (values == null || values.isEmpty()) {
            return null;
        }
        Object nodeId = values.get("nodeId");
        Object connectionId = values.get("connectionId");
        if (nodeId == null || connectionId == null) {
            return null;
        }
        return new UserPresence(userId, nodeId.toString(), connectionId.toString());
    }

    public boolean registerRequestNonce(String nodeId, String nonce) {
        if (nodeId == null || nonce == null) {
            return false;
        }
        Boolean accepted = redisTemplate.opsForValue().setIfAbsent(
                NONCE_PREFIX + nodeId + ":" + nonce,
                "1",
                properties.getInternalRequestMaxAgeSeconds(),
                TimeUnit.SECONDS);
        return Boolean.TRUE.equals(accepted);
    }

    public boolean isPushEventConsumed(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return true;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(consumedEventKey(eventId)));
    }

    public void markPushEventConsumed(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return;
        }
        redisTemplate.opsForValue().set(
                consumedEventKey(eventId),
                "1",
                24,
                TimeUnit.HOURS);
    }

    private String activeKey(Integer userId) { return ACTIVE_PREFIX + userId; }
    private String connectionKey(String connectionId) { return CONNECTION_PREFIX + connectionId; }
    private String consumedEventKey(String eventId) { return CONSUMED_EVENT_PREFIX + properties.getNodeId() + ":" + eventId; }
}
