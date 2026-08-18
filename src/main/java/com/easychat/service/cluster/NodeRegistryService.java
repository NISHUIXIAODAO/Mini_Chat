package com.easychat.service.cluster;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.cluster.ClusterNode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class NodeRegistryService {
    private static final String NODE_PREFIX = "cluster:node:";
    private static final String NODES_KEY = "cluster:nodes";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ClusterNodeProperties properties;

    public NodeRegistryService(RedisTemplate<String, Object> redisTemplate, ClusterNodeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void registerCurrentNode() {
        register(properties.getNodeId(), properties.getInternalHost(), properties.getInternalPort(), properties.getWsPort());
    }

    @Scheduled(fixedDelayString = "${easychat.cluster.node-heartbeat-seconds:10}000")
    public void heartbeat() {
        registerCurrentNode();
    }

    @PreDestroy
    public void unregisterCurrentNode() {
        redisTemplate.delete(nodeKey(properties.getNodeId()));
        redisTemplate.opsForSet().remove(NODES_KEY, properties.getNodeId());
    }

    public void register(String nodeId, String internalHost, int internalPort, int wsPort) {
        Map<String, Object> values = new HashMap<>();
        values.put("nodeId", nodeId);
        values.put("internalHost", internalHost);
        values.put("internalPort", internalPort);
        values.put("wsPort", wsPort);
        values.put("status", "UP");
        values.put("lastHeartbeat", System.currentTimeMillis());
        redisTemplate.opsForHash().putAll(nodeKey(nodeId), values);
        redisTemplate.expire(nodeKey(nodeId), properties.getNodeTtlSeconds(), TimeUnit.SECONDS);
        redisTemplate.opsForSet().add(NODES_KEY, nodeId);
    }

    public ClusterNode findHealthyNode(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        String key = nodeKey(nodeId);
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
        if (values == null || values.isEmpty() || !"UP".equals(String.valueOf(values.get("status")))) {
            redisTemplate.opsForSet().remove(NODES_KEY, nodeId);
            return null;
        }
        try {
            return new ClusterNode(nodeId,
                    String.valueOf(values.get("internalHost")),
                    Integer.parseInt(String.valueOf(values.get("internalPort"))),
                    Integer.parseInt(String.valueOf(values.get("wsPort"))));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String nodeKey(String nodeId) {
        return NODE_PREFIX + nodeId;
    }
}
