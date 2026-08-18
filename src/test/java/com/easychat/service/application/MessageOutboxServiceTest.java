package com.easychat.service.application;

import com.easychat.entity.DO.MessageOutbox;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.event.PushEvent;
import com.easychat.mapper.MessageOutboxMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MessageOutboxServiceTest {
    @Test
    public void createChatMessagePersistsPendingOutboxWithCompleteEvent() {
        MessageOutboxMapper mapper = mock(MessageOutboxMapper.class);
        MessageOutboxService service = new MessageOutboxService(mapper);
        MessageSendDTO<String> message = new MessageSendDTO<>();
        message.setMessageId(99L).setMessageContent("hello");

        PushEvent event = service.createChatMessage(99L, "P_1_2", Arrays.asList(2, 3), message);

        ArgumentCaptor<MessageOutbox> captor = ArgumentCaptor.forClass(MessageOutbox.class);
        verify(mapper).insert(captor.capture());
        MessageOutbox outbox = captor.getValue();
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(99L, outbox.getAggregateId().longValue());
        assertEquals(event.getEventId(), outbox.getEventId());
        assertNotNull(outbox.getPayload());
    }
}
