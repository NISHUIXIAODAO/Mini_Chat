package com.easychat.service.impl;

import com.easychat.entity.DO.UserInfo;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IJWTService;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class JWTServiceImpl implements IJWTService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    private final String JWT_Token = "gdsufgdouwefghoiwehfoliewhuiotrhiovhewiofherwiuhgipwerhbvierhjh";

    /***
     * 生成 token
     * @param userId
     * @return
     */
    public String generateToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        String jwt = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256, JWT_Token)//签名算法
                .setClaims(claims)//自定义内容
                .setExpiration(new Date(System.currentTimeMillis() + 24L * 60 * 60 * 1000))
                .compact();
        return jwt;
    }

    /***
     * 验证 token的有效性 todo
     * @param token
     * @return
     */
    public boolean checkToken(String token) {
        try {
            return true;
        } catch (ExpiredJwtException e) {
            // 捕获 Token 过期异常
            log.error("Token 已过期");
            return false;
        } catch (SignatureException e) {
            // 捕获签名不匹配（密钥错误或数据篡改）
            log.error("密钥错误或数据篡改");
            return false;
        } catch (MalformedJwtException e) {
            // 捕获 Token 格式错误（如缺少部分、编码问题）
            log.error("捕获 Token 格式错误");
            return false;
        } catch (UnsupportedJwtException e) {
            // 捕获不支持的 JWT 类型（如算法不匹配）
            log.error("捕获不支持的 JWT 类型");
            return false;
        } catch (IllegalArgumentException e) {
            // 捕获空 Token 或无效参数
            log.error("捕获空 Token 或无效参数");
            return false;
        } catch (Exception e) {
            // 兜底处理其他未知异常
            log.error("其他未知异常");
            return false;
        }
    }

    /***
     * 解析 token拿到 claims
     * @param token
     * @return
     */
    public Claims parseJWT(String token) {
        return Jwts.parser()
                .setSigningKey(JWT_Token)
                .parseClaimsJws(token)
                .getBody();
    }

    /***
     * 查询用户ID
     * @param token
     * @return
     */
    public Integer getUserId(String token) {
        if (token != null) {
            return (Integer) parseJWT(token).get("userId");
        }
        return null;
    }
    /***
     * 通过 token查询用户 ID，并进行数据库校验
     * @param token
     * @return
     */
    public Boolean verifyToken(String token){
        //检查token有效性
        if(!checkToken(token)){
            return false;
        }
        //解析token
        int userId = getUserId(token);
        //查询用户是否存在数据库中
        UserInfo user = userInfoMapper.getUserById(userId);
        return user != null;
    }
}
