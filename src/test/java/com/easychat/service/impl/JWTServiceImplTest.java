package com.easychat.service.impl;

import com.easychat.entity.DO.UserInfo;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IRedisService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JWTServiceImplTest {

    private UserInfoMapper userInfoMapper;
    private IRedisService redisService;
    private JWTServiceImpl jwtService;

    @BeforeEach
    public void setUp() {
        userInfoMapper = mock(UserInfoMapper.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        redisService = mock(IRedisService.class);
        jwtService = new JWTServiceImpl(userInfoMapper, redisTemplate, redisService);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtService, "expirationMillis", 60000L);
    }

    @Test
    public void generateTokenShouldPersistCurrentAuthSession() {
        String token = jwtService.generateToken(1001);
        String sessionId = jwtService.getSessionId(token);

        Assertions.assertNotNull(sessionId);
        verify(redisService).saveAuthSession(1001, sessionId, 60000L);
    }

    @Test
    public void verifyTokenShouldRejectStaleAuthSession() {
        String token = jwtService.generateToken(1001);
        when(redisService.isCurrentAuthSession(eq(1001), anyString())).thenReturn(false);
        when(userInfoMapper.getUserById(1001)).thenReturn(new UserInfo());

        Assertions.assertFalse(jwtService.verifyToken(token));
    }

    @Test
    public void verifyTokenShouldAcceptCurrentAuthSession() {
        String token = jwtService.generateToken(1001);
        Claims claims = jwtService.parseJWT(token);
        String sessionId = claims.get("sessionId").toString();
        when(redisService.isCurrentAuthSession(1001, sessionId)).thenReturn(true);
        when(userInfoMapper.getUserById(1001)).thenReturn(new UserInfo());

        Assertions.assertTrue(jwtService.verifyToken(token));
    }
}
