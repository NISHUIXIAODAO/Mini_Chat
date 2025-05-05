package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageProducer {

    private static final String TOPIC = "message-topic";

    private final KafkaTemplate<String, MessageSendDTO> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, MessageSendDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(MessageSendDTO message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
