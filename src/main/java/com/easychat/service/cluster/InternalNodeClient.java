package com.easychat.service.cluster;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.cluster.ClusterNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class InternalNodeClient {
    private final NodeRegistryService nodeRegistryService;
    private final ClusterNodeProperties properties;
    private final InternalRequestSigner signer;

    public InternalNodeClient(NodeRegistryService nodeRegistryService,
                              ClusterNodeProperties properties,
                              InternalRequestSigner signer) {
        this.nodeRegistryService = nodeRegistryService;
        this.properties = properties;
        this.signer = signer;
    }

    public boolean push(String nodeId, Integer userId, MessageSendDTO<?> message) {
        return post(nodeId, "/internal/push?userId=" + userId, JSON.toJSONString(message), "application/json");
    }

    public boolean forceOffline(String nodeId, Integer userId, String reason) {
        return post(nodeId, "/internal/offline?userId=" + userId, reason == null ? "" : reason, "text/plain;charset=UTF-8");
    }

    private boolean post(String nodeId, String requestTarget, String body, String contentType) {
        ClusterNode node = nodeRegistryService.findHealthyNode(nodeId);
        if (node == null || !signer.isConfigured()) {
            log.warn("无法调用内部节点，nodeId={}，节点不可用或内部密钥未配置", nodeId);
            return false;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String signature = signer.sign("POST", requestTarget, timestamp, nonce, body.getBytes(StandardCharsets.UTF_8));
        String url = "http://" + node.getInternalHost() + ":" + node.getInternalPort() + requestTarget;
        try {
            return HttpRequest.post(url)
                    .header("Content-Type", contentType)
                    .header("X-Cluster-Node-Id", properties.getNodeId())
                    .header("X-Cluster-Timestamp", timestamp)
                    .header("X-Cluster-Nonce", nonce)
                    .header("X-Cluster-Signature", signature)
                    .body(body)
                    .timeout(2000)
                    .execute()
                    .isOk();
        } catch (Exception e) {
            log.error("内部节点调用失败，nodeId={}, url={}", nodeId, url, e);
            return false;
        }
    }
}
