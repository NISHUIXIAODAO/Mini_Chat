package com.easychat.service.application;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.service.IRedisService;
import com.easychat.webSocket.ChannelContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MessagePushService {

    private final IRedisService redisService;
    private final ChannelContextUtils channelContextUtils;

    @Value("${server.port:5050}")
    private String serverPort;

    private final String localIp;

    public MessagePushService(IRedisService redisService,
                              ChannelContextUtils channelContextUtils) {
        this.redisService = redisService;
        this.channelContextUtils = channelContextUtils;
        this.localIp = resolveLocalIp();
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
            String targetAddress = redisService.getActiveUserLocation(userId);
            log.info("准备推送消息给用户: {}, Redis记录地址: {}, 本机地址: {}:{}", userId, targetAddress, localIp, serverPort);

            if (targetAddress == null) {
                log.info("用户 {} 不在线 (Redis无位置记录)", userId);
                return;
            }

            String[] parts = targetAddress.split(":");
            String targetIp = parts[0];
            String targetPort = parts.length > 1 ? parts[1] : "5050";

            boolean isLocalIp = targetIp.equals(localIp) || targetIp.equals("127.0.0.1") || targetIp.equals("localhost");
            boolean isLocalPort = targetPort.equals(serverPort);

            if (isLocalIp && isLocalPort) {
                log.info("目标用户在【本机】，直接通过WebSocket推送");
                pushLocal(userId, message);
                return;
            }

            final String url = "http://" + targetIp + ":" + targetPort + "/internal/push?userId=" + userId;
            log.info("目标用户在【远程节点】({}:{}), 发起HTTP转发: {}", targetIp, targetPort, url);
            CompletableFuture.runAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpRequest.post(url)
                                .body(JSONUtil.toJsonStr(message))
                                .timeout(2000)
                                .execute();
                    } catch (Exception e) {
                        log.error("HTTP转发消息失败", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("消息推送失败", e);
        }
    }

    private void pushLocal(Integer userId, MessageSendDTO<?> message) {
        channelContextUtils.sendMsg(message, userId);
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

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
