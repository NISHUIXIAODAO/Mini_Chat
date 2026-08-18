package com.easychat.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaPushConfig {
    @Bean
    public NewTopic easychatPushTopic(KafkaPushProperties properties) {
        return new NewTopic(properties.getPushTopic(), 1, (short) 1);
    }

    @Bean
    public String pushConsumerGroup(KafkaPushProperties kafkaProperties, ClusterNodeProperties clusterProperties) {
        return kafkaProperties.getConsumerGroupPrefix() + "-" + clusterProperties.getNodeId();
    }

    @Bean(name = "pushKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> pushKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, KafkaPushProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(properties.getConsumerRetryIntervalMillis(), properties.getConsumerRetryAttempts())));
        return factory;
    }
}
