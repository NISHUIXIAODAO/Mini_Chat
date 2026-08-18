package com.easychat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "easychat.cluster")
public class ClusterNodeProperties {
    private String nodeId;
    private String internalHost = "127.0.0.1";
    private int internalPort = 5050;
    private int wsPort = 5051;
    private long nodeTtlSeconds = 30L;
    private long nodeHeartbeatSeconds = 10L;
    private long presenceTtlSeconds = 90L;
    private String internalSecret;
    private String internalAllowedIps = "127.0.0.1,::1";
    private long internalRequestMaxAgeSeconds = 30L;

    public String getNodeId() {
        if (StringUtils.hasText(nodeId)) {
            return nodeId.trim();
        }
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + internalPort;
        } catch (Exception e) {
            return "localhost-" + internalPort;
        }
    }

    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getInternalHost() { return internalHost; }
    public void setInternalHost(String internalHost) { this.internalHost = internalHost; }
    public int getInternalPort() { return internalPort; }
    public void setInternalPort(int internalPort) { this.internalPort = internalPort; }
    public int getWsPort() { return wsPort; }
    public void setWsPort(int wsPort) { this.wsPort = wsPort; }
    public long getNodeTtlSeconds() { return nodeTtlSeconds; }
    public void setNodeTtlSeconds(long nodeTtlSeconds) { this.nodeTtlSeconds = nodeTtlSeconds; }
    public long getNodeHeartbeatSeconds() { return nodeHeartbeatSeconds; }
    public void setNodeHeartbeatSeconds(long nodeHeartbeatSeconds) { this.nodeHeartbeatSeconds = nodeHeartbeatSeconds; }
    public long getPresenceTtlSeconds() { return presenceTtlSeconds; }
    public void setPresenceTtlSeconds(long presenceTtlSeconds) { this.presenceTtlSeconds = presenceTtlSeconds; }
    public String getInternalSecret() { return internalSecret; }
    public void setInternalSecret(String internalSecret) { this.internalSecret = internalSecret; }
    public void setInternalAllowedIps(String internalAllowedIps) { this.internalAllowedIps = internalAllowedIps; }
    public long getInternalRequestMaxAgeSeconds() { return internalRequestMaxAgeSeconds; }
    public void setInternalRequestMaxAgeSeconds(long internalRequestMaxAgeSeconds) { this.internalRequestMaxAgeSeconds = internalRequestMaxAgeSeconds; }

    public List<String> getInternalAllowedIps() {
        if (!StringUtils.hasText(internalAllowedIps)) {
            return new ArrayList<>();
        }
        return Arrays.stream(internalAllowedIps.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
