package com.easychat.service;

import io.jsonwebtoken.Claims;

import javax.servlet.http.HttpServletRequest;

public interface IJWTService {
    public String generateToken(Integer userId);
    public boolean checkToken(String token);
    public Claims parseJWT(String token);
    public Integer getUserId(String token);
    public String getSessionId(String token);
    public Boolean verifyToken(String token);
    public String extractToken(HttpServletRequest request);
    public void blacklistToken(String token);
    public void removeCurrentSession(String token);
}
