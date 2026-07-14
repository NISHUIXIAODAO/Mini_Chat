package com.easychat.service.application;

import cn.hutool.http.HttpRequest;
import com.easychat.service.IRedisService;
import com.easychat.webSocket.ChannelContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
@Slf4j
@Service
public class UserOnlineService {

    private final IRedisService redisService;
    private final ChannelContextUtils channelContextUtils;

    @Value("${server.port:5050}")
    private String serverPort;

    private final String localIp;

    public UserOnlineService(IRedisService redisService,
                             ChannelContextUtils channelContextUtils) {
        this.redisService = redisService;
        this.channelContextUtils = channelContextUtils;
        this.localIp = resolveLocalIp();
    }

    public boolean forceOffline(Integer userId, String reason) {
        if (userId == null) {
            return false;
        }
        String targetAddress = redisService.getActiveUserLocation(userId);
        if (targetAddress == null) {
            return channelContextUtils.forceOffline(userId, reason);
        }
        String[] parts = targetAddress.split(":");
        String targetIp = parts[0];
        String targetPort = parts.length > 1 ? parts[1] : "5050";

        boolean isLocalIp = targetIp.equals(localIp) || targetIp.equals("127.0.0.1") || targetIp.equals("localhost");
        boolean isLocalPort = targetPort.equals(serverPort);
        if (isLocalIp && isLocalPort) {
            return channelContextUtils.forceOffline(userId, reason);
        }

        String url = "http://" + targetIp + ":" + targetPort + "/internal/offline?userId=" + userId;
        try {
            return HttpRequest.post(url)
                    .body(reason == null ? "" : reason)
                    .timeout(2000)
                    .execute()
                    .isOk();
        } catch (Exception e) {
            log.error("远程强制下线失败, userId: {}, url: {}", userId, url, e);
            return false;
        }
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
