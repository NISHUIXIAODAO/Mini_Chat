package com.easychat.service.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthSessionService {
    private static final String PREFIX = "auth:session:";
    private final RedisTemplate<String, Object> redisTemplate;

    public AuthSessionService(RedisTemplate<String, Object> redisTemplate) { this.redisTemplate = redisTemplate; }

    public void save(Integer userId, String sessionId, long ttlMillis) {
        if (userId != null && sessionId != null && ttlMillis > 0) {
            redisTemplate.opsForValue().set(key(userId), sessionId, ttlMillis, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isCurrent(Integer userId, String sessionId) {
        Object current = userId == null ? null : redisTemplate.opsForValue().get(key(userId));
        return sessionId != null && sessionId.equals(current);
    }

    public void removeIfCurrent(Integer userId, String sessionId) {
        if (isCurrent(userId, sessionId)) { redisTemplate.delete(key(userId)); }
    }

    private String key(Integer userId) { return PREFIX + userId; }
}
