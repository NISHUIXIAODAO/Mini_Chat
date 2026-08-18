package com.easychat.service.impl;

import com.easychat.entity.DO.UserInfo;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.auth.AuthSessionService;
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
    private AuthSessionService authSessionService;
    private JWTServiceImpl jwtService;

    @BeforeEach
    public void setUp() {
        userInfoMapper = mock(UserInfoMapper.class);
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        authSessionService = mock(AuthSessionService.class);
        jwtService = new JWTServiceImpl(userInfoMapper, redisTemplate, authSessionService);
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtService, "expirationMillis", 60000L);
    }

    @Test
    public void generateTokenShouldPersistCurrentAuthSession() {
        String token = jwtService.generateToken(1001);
        Assertions.assertNotNull(jwtService.getSessionId(token));
        verify(authSessionService).save(eq(1001), anyString(), eq(60000L));
    }

    @Test
    public void verifyTokenShouldRejectStaleAuthSession() {
        String token = jwtService.generateToken(1001);
        when(authSessionService.isCurrent(eq(1001), anyString())).thenReturn(false);
        when(userInfoMapper.getUserById(1001)).thenReturn(new UserInfo());
        Assertions.assertFalse(jwtService.verifyToken(token));
    }

    @Test
    public void verifyTokenShouldAcceptCurrentAuthSession() {
        String token = jwtService.generateToken(1001);
        when(authSessionService.isCurrent(eq(1001), anyString())).thenReturn(true);
        when(userInfoMapper.getUserById(1001)).thenReturn(new UserInfo());
        Assertions.assertTrue(jwtService.verifyToken(token));
    }
}
