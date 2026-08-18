package com.easychat.service.auth;

import com.easychat.entity.DTO.response.WebSocketTicketResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class WebSocketConnectionTicketServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private AuthSessionService authSessionService;
    private WebSocketConnectionTicketService service;

    @BeforeEach
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        authSessionService = mock(AuthSessionService.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new WebSocketConnectionTicketService(redisTemplate, authSessionService);
        ReflectionTestUtils.setField(service, "ticketTtlSeconds", 60L);
    }

    @Test
    public void issueStoresOnlyHashedTicketKey() {
        WebSocketTicketResponseDTO response = service.issue(1001, "session-a");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(key.capture(), eq("1001|session-a"), eq(60L), eq(TimeUnit.SECONDS));
        assertTrue(response.getTicket().matches("[A-Za-z0-9_-]{43}"));
        assertTrue(key.getValue().startsWith("ws:ticket:"));
        assertNotEquals(key.getValue(), "ws:ticket:" + response.getTicket());
        assertEquals(60000L, response.getExpiresInMillis());
    }

    @Test
    public void consumeDeletesTicketAndReturnsUserForCurrentSession() {
        String ticket = ticket('A');
        when(valueOperations.getAndDelete(anyString())).thenReturn("1001|session-a");
        when(authSessionService.isCurrent(1001, "session-a")).thenReturn(true);

        assertEquals(Integer.valueOf(1001), service.consume(ticket));
        verify(valueOperations).getAndDelete(anyString());
        verify(authSessionService).isCurrent(1001, "session-a");
    }

    @Test
    public void consumeRejectsInvalidTicketBeforeRedisAccess() {
        assertNull(service.consume("invalid"));
        verifyNoInteractions(valueOperations, authSessionService);
    }

    @Test
    public void consumeRejectsTicketForStaleSession() {
        String ticket = ticket('B');
        when(valueOperations.getAndDelete(anyString())).thenReturn("1001|session-a");
        when(authSessionService.isCurrent(1001, "session-a")).thenReturn(false);

        assertNull(service.consume(ticket));
    }

    private String ticket(char character) {
        StringBuilder builder = new StringBuilder(43);
        for (int i = 0; i < 43; i++) {
            builder.append(character);
        }
        return builder.toString();
    }
}
