package com.easychat.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RedisServiceImplTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private RedisServiceImpl redisService;

    @BeforeEach
    public void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        redisService = new RedisServiceImpl(redisTemplate);
    }

    @Test
    public void saveAuthSessionShouldUseJwtTtl() {
        redisService.saveAuthSession(1001, "session-a", 60000L);

        verify(valueOperations).set(eq("auth:session:1001"), eq("session-a"), eq(60000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    public void isCurrentAuthSessionShouldCompareStoredSession() {
        when(valueOperations.get("auth:session:1001")).thenReturn("session-a");

        Assertions.assertTrue(redisService.isCurrentAuthSession(1001, "session-a"));
        Assertions.assertFalse(redisService.isCurrentAuthSession(1001, "session-b"));
    }

    @Test
    public void getActiveUserLocationShouldReadLocationForActiveConnection() {
        when(valueOperations.get("ws:user:active:1001")).thenReturn("abc");
        when(valueOperations.get("ws:location:1001:abc")).thenReturn("127.0.0.1:5050:abc");

        String location = redisService.getActiveUserLocation(1001);

        Assertions.assertEquals("127.0.0.1:5050:abc", location);
    }

    @Test
    public void removeOnlineConnectionShouldNotRemoveActiveConnectionWhenClosingOldConnection() {
        when(valueOperations.get("ws:user:active:1001")).thenReturn("new");

        redisService.removeOnlineConnection(1001, "old");

        verify(redisTemplate).delete("ws:online:1001:old");
        verify(redisTemplate).delete("ws:location:1001:old");
        verify(redisTemplate, never()).delete("ws:user:active:1001");
    }

    @Test
    public void saveOnlineConnectionShouldSetActiveConnectionWithTtl() {
        redisService.saveOnlineConnection(1001, "abc", "127.0.0.1:5050:abc");

        verify(valueOperations).set(eq("ws:user:active:1001"), eq("abc"), eq(90L), eq(TimeUnit.SECONDS));
        verify(valueOperations).set(eq("ws:location:1001:abc"), eq("127.0.0.1:5050:abc"), eq(90L), eq(TimeUnit.SECONDS));
    }
}
