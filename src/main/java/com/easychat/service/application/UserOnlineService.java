package com.easychat.service.application;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.cluster.UserPresence;
import com.easychat.service.cluster.InternalNodeClient;
import com.easychat.service.cluster.UserPresenceService;
import com.easychat.webSocket.LocalChannelRegistry;
import org.springframework.stereotype.Service;

@Service
public class UserOnlineService {
    private final UserPresenceService userPresenceService;
    private final ClusterNodeProperties properties;
    private final LocalChannelRegistry localChannelRegistry;
    private final InternalNodeClient internalNodeClient;

    public UserOnlineService(UserPresenceService userPresenceService,
                             ClusterNodeProperties properties,
                             LocalChannelRegistry localChannelRegistry,
                             InternalNodeClient internalNodeClient) {
        this.userPresenceService = userPresenceService;
        this.properties = properties;
        this.localChannelRegistry = localChannelRegistry;
        this.internalNodeClient = internalNodeClient;
    }

    public boolean forceOffline(Integer userId, String reason) {
        if (userId == null) {
            return false;
        }
        UserPresence presence = userPresenceService.findActive(userId);
        if (presence == null) {
            return localChannelRegistry.forceOffline(userId, reason);
        }
        if (properties.getNodeId().equals(presence.getNodeId())) {
            return localChannelRegistry.forceOffline(userId, reason);
        }
        return internalNodeClient.forceOffline(presence.getNodeId(), userId, reason);
    }
}
