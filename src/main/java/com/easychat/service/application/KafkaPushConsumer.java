package com.easychat.service.application;

import com.easychat.config.KafkaPushProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaPushConsumer {
    private final PushDispatcher pushDispatcher;

    public KafkaPushConsumer(PushDispatcher pushDispatcher) {
        this.pushDispatcher = pushDispatcher;
    }

    @KafkaListener(topics = "${easychat.kafka.push-topic}", groupId = "#{pushConsumerGroup}",
            containerFactory = "pushKafkaListenerContainerFactory")
    public void consume(String payload, Acknowledgment acknowledgment) {
        pushDispatcher.dispatch(payload);
        acknowledgment.acknowledge();
    }
}
