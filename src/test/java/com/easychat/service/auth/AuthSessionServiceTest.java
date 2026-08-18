package com.easychat.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthSessionServiceTest {
    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private AuthSessionService service;

    @BeforeEach
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new AuthSessionService(redisTemplate);
    }

    @Test
    public void saveUsesJwtTtl() {
        service.save(1001, "session-a", 60000L);
        verify(valueOperations).set(eq("auth:session:1001"), eq("session-a"), eq(60000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void removeOnlyDeletesCurrentSession() {
        when(valueOperations.get("auth:session:1001")).thenReturn("session-a");
        assertTrue(service.isCurrent(1001, "session-a"));
        assertFalse(service.isCurrent(1001, "session-b"));
        service.removeIfCurrent(1001, "session-a");
        verify(redisTemplate).delete("auth:session:1001");
    }
}
