package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageProducer.class);
    private static final String TOPIC = "message-topic";

    private final KafkaTemplate<String, MessageSendDTO> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, MessageSendDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(MessageSendDTO message) {
        logger.info("发送消息到Topic：{}", message.getMessageContent());
        kafkaTemplate.send(TOPIC, message);
    }
}
