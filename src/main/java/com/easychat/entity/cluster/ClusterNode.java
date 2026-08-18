package com.easychat.entity.cluster;

public class ClusterNode {
    private final String nodeId;
    private final String internalHost;
    private final int internalPort;
    private final int wsPort;

    public ClusterNode(String nodeId, String internalHost, int internalPort, int wsPort) {
        this.nodeId = nodeId;
        this.internalHost = internalHost;
        this.internalPort = internalPort;
        this.wsPort = wsPort;
    }

    public String getNodeId() { return nodeId; }
    public String getInternalHost() { return internalHost; }
    public int getInternalPort() { return internalPort; }
    public int getWsPort() { return wsPort; }
}
