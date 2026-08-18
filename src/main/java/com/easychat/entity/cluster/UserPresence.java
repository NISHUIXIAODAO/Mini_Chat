package com.easychat.entity.cluster;

public class UserPresence {
    private final Integer userId;
    private final String nodeId;
    private final String connectionId;

    public UserPresence(Integer userId, String nodeId, String connectionId) {
        this.userId = userId;
        this.nodeId = nodeId;
        this.connectionId = connectionId;
    }

    public Integer getUserId() { return userId; }
    public String getNodeId() { return nodeId; }
    public String getConnectionId() { return connectionId; }
}
