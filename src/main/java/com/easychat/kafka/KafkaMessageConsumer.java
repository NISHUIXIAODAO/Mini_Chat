package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.utils.GroupIdTools;
import com.easychat.webSocket.ChannelContextUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaMessageConsumer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageConsumer.class);
    private final ChannelContextUtils channelContextUtils;

    @KafkaListener(
            topics = "message-topic",
            groupId = "#{T(com.easychat.utils.GroupIdTools).uniqueConsumerGroupId()}")
    // 服务启动时候，监听KafkaTopic
    public void listen(MessageSendDTO message) {
        // 接收到 Kafka 的消息后，转发给目标客户端
        logger.info("收到广播消息：{}", message.getMessageContent());
        channelContextUtils.sendMessage(message);
    }
}


