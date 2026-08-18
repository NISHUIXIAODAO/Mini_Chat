package com.easychat.service.application;

import com.easychat.config.ClusterNodeProperties;
import com.easychat.config.KafkaPushProperties;
import com.easychat.config.OutboxProperties;
import com.easychat.entity.DO.MessageOutbox;
import com.easychat.mapper.MessageOutboxMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OutboxPublisherTest {
    @Test
    public void failureReturnsPendingRecordWithExponentialBackoff() {
        MessageOutboxMapper mapper = mock(MessageOutboxMapper.class);
        OutboxProperties properties = new OutboxProperties();
        properties.setInitialBackoffMillis(100L);
        properties.setMaxBackoffMillis(1000L);
        properties.setMaxRetryCount(3);
        OutboxPublisher publisher = publisher(mapper, properties);
        MessageOutbox outbox = new MessageOutbox().setId(1L).setRetryCount(0)
                .setTraceId("trace").setEventId("event").setAggregateId(1L);

        publisher.markFailure(outbox, new RuntimeException("broker down"));

        ArgumentCaptor<Long> retryAt = ArgumentCaptor.forClass(Long.class);
        verify(mapper).markFailure(eq(1L), eq("PENDING"), eq(1), retryAt.capture(), eq("RuntimeException: broker down"), org.mockito.ArgumentMatchers.anyLong());
        assertTrue(retryAt.getValue() >= System.currentTimeMillis() + 90L);
        assertEquals(400L, publisher.backoffMillis(3));
    }

    @Test
    public void maxRetriesMarksRecordFailed() {
        MessageOutboxMapper mapper = mock(MessageOutboxMapper.class);
        OutboxProperties properties = new OutboxProperties();
        properties.setMaxRetryCount(3);
        OutboxPublisher publisher = publisher(mapper, properties);
        MessageOutbox outbox = new MessageOutbox().setId(1L).setRetryCount(2)
                .setTraceId("trace").setEventId("event").setAggregateId(1L);

        publisher.markFailure(outbox, new RuntimeException("broker down"));

        verify(mapper).markFailure(eq(1L), eq("FAILED"), eq(3), org.mockito.ArgumentMatchers.anyLong(),
                eq("RuntimeException: broker down"), org.mockito.ArgumentMatchers.anyLong());
    }

    private OutboxPublisher publisher(MessageOutboxMapper mapper, OutboxProperties properties) {
        ClusterNodeProperties clusterProperties = new ClusterNodeProperties();
        clusterProperties.setNodeId("node-a");
        return new OutboxPublisher(mapper, mock(KafkaTemplate.class), new KafkaPushProperties(), properties, clusterProperties);
    }
}
