package com.easychat.service.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VerificationCodeService {
    private final RedisTemplate<String, Object> redisTemplate;
    public VerificationCodeService(RedisTemplate<String, Object> redisTemplate) { this.redisTemplate = redisTemplate; }
    public String get(String email) {
        Object code = redisTemplate.opsForValue().get(email);
        return code == null ? null : code.toString();
    }
    public void save(String email, String code, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(email, code, timeout, timeUnit);
    }
}
