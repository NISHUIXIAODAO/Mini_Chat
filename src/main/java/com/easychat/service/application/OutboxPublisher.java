package com.easychat.service.application;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.config.KafkaPushProperties;
import com.easychat.config.OutboxProperties;
import com.easychat.entity.DO.MessageOutbox;
import com.easychat.enums.OutboxStatusEnum;
import com.easychat.mapper.MessageOutboxMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OutboxPublisher {
    private final MessageOutboxMapper messageOutboxMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaPushProperties kafkaProperties;
    private final OutboxProperties outboxProperties;
    private final ClusterNodeProperties clusterProperties;

    public OutboxPublisher(MessageOutboxMapper messageOutboxMapper,
                           KafkaTemplate<String, String> kafkaTemplate,
                           KafkaPushProperties kafkaProperties,
                           OutboxProperties outboxProperties,
                           ClusterNodeProperties clusterProperties) {
        this.messageOutboxMapper = messageOutboxMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.outboxProperties = outboxProperties;
        this.clusterProperties = clusterProperties;
    }

    @Scheduled(fixedDelayString = "${easychat.outbox.scan-interval-millis:1000}")
    public void publishPendingEvents() {
        long now = System.currentTimeMillis();
        List<MessageOutbox> candidates = messageOutboxMapper.findPublishable(now, outboxProperties.getBatchSize());
        for (MessageOutbox candidate : candidates) {
            if (messageOutboxMapper.claim(candidate.getId(), now, now + TimeUnit.SECONDS.toMillis(outboxProperties.getLeaseSeconds())) == 1) {
                publish(candidate);
            }
        }
    }

    void publish(MessageOutbox outbox) {
        try {
            log.info("outbox publish start traceId={}, eventId={}, messageId={}, nodeId={}",
                    outbox.getTraceId(), outbox.getEventId(), outbox.getAggregateId(), clusterProperties.getNodeId());
            kafkaTemplate.send(kafkaProperties.getPushTopic(), String.valueOf(outbox.getAggregateId()), outbox.getPayload())
                    .get(10, TimeUnit.SECONDS);
            messageOutboxMapper.markPublished(outbox.getId(), System.currentTimeMillis());
            log.info("outbox publish success traceId={}, eventId={}, messageId={}, nodeId={}",
                    outbox.getTraceId(), outbox.getEventId(), outbox.getAggregateId(), clusterProperties.getNodeId());
        } catch (Exception e) {
            markFailure(outbox, e);
        }
    }

    void markFailure(MessageOutbox outbox, Exception exception) {
        int retries = outbox.getRetryCount() + 1;
        long now = System.currentTimeMillis();
        boolean failed = retries >= outboxProperties.getMaxRetryCount();
        long retryAt = failed ? now : now + backoffMillis(retries);
        String error = exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage());
        if (error.length() > 1000) {
            error = error.substring(0, 1000);
        }
        messageOutboxMapper.markFailure(outbox.getId(), failed ? OutboxStatusEnum.FAILED.name() : OutboxStatusEnum.PENDING.name(),
                retries, retryAt, error, now);
        log.error("outbox publish failed traceId={}, eventId={}, messageId={}, retryCount={}, nodeId={}",
                outbox.getTraceId(), outbox.getEventId(), outbox.getAggregateId(), retries, clusterProperties.getNodeId(), exception);
    }

    long backoffMillis(int retryCount) {
        long multiplier = 1L << Math.min(retryCount - 1, 20);
        return Math.min(outboxProperties.getMaxBackoffMillis(), outboxProperties.getInitialBackoffMillis() * multiplier);
    }
}
