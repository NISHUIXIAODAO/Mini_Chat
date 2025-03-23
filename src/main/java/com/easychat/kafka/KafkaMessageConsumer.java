package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.utils.JsonUtils;
import com.easychat.webSocket.ChannelContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final ChannelContextUtils channelContextUtils;
    public KafkaMessageConsumer(ChannelContextUtils channelContextUtils) {
        this.channelContextUtils = channelContextUtils;
    }
    @KafkaListener(topics = "message-topic", groupId = "group1")
    public void listen(MessageSendDTO message) {
        // 接收到 Kafka 的消息后，转发给目标客户端
        logger.info("收到广播消息：{}", message);
        channelContextUtils.sendMessage(message);
    }
}


