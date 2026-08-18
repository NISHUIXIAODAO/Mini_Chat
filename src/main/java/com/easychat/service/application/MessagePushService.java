package com.easychat.service.application;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.cluster.UserPresence;
import com.easychat.service.cluster.InternalNodeClient;
import com.easychat.service.cluster.UserPresenceService;
import com.easychat.webSocket.LocalChannelRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class MessagePushService {
    private final UserPresenceService userPresenceService;
    private final ClusterNodeProperties properties;
    private final LocalChannelRegistry localChannelRegistry;
    private final InternalNodeClient internalNodeClient;

    public MessagePushService(UserPresenceService userPresenceService,
                              ClusterNodeProperties properties,
                              LocalChannelRegistry localChannelRegistry,
                              InternalNodeClient internalNodeClient) {
        this.userPresenceService = userPresenceService;
        this.properties = properties;
        this.localChannelRegistry = localChannelRegistry;
        this.internalNodeClient = internalNodeClient;
    }

    public void pushToUserAfterCommit(final Integer userId, final MessageSendDTO<?> message) {
        afterCommit(new Runnable() {
            @Override
            public void run() {
                pushToUser(userId, message);
            }
        });
    }

    public void pushToUser(Integer userId, MessageSendDTO<?> message) {
        try {
            UserPresence presence = userPresenceService.findActive(userId);
            if (presence == null) {
                // Redis presence 过期时仍保留本机活动 Channel 的单节点兼容性。
                if (!localChannelRegistry.send(message, userId)) {
                    log.info("用户 {} 不在线", userId);
                }
                return;
            }
            if (properties.getNodeId().equals(presence.getNodeId())) {
                localChannelRegistry.send(message, userId);
                return;
            }
            internalNodeClient.push(presence.getNodeId(), userId, message);
        } catch (Exception e) {
            log.error("消息推送失败, userId={}", userId, e);
        }
    }

    public void afterCommit(final Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (Exception e) {
                    log.error("事务提交后的消息推送失败", e);
                }
            }
        });
    }
}
