package com.easychat.service;

import io.jsonwebtoken.Claims;

public interface IJWTService {
    public String generateToken(Integer userId);
    public boolean checkToken(String token);
    public Claims parseJWT(String token);
    public Integer getUserId(String token);
    public Boolean verifyToken(String token);
}
