package com.easychat.utils;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class GroupIdTools {
    public static String uniqueConsumerGroupId() {
        // 格式: 服务名-随机UUID，确保唯一性
        return "easychat-service" + UUID.randomUUID().toString();
    }
}
