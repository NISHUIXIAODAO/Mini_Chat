package com.easychat.service.impl;

import com.easychat.entity.DO.UserInfo;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IJWTService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class JWTServiceImpl implements IJWTService {
    private final UserInfoMapper userInfoMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-millis:86400000}")
    private Long expirationMillis;

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    public JWTServiceImpl(UserInfoMapper userInfoMapper, RedisTemplate<String, Object> redisTemplate) {
        this.userInfoMapper = userInfoMapper;
        this.redisTemplate = redisTemplate;
    }

    /***
     * 生成 token
     * @param userId
     * @return
     */
    public String generateToken(Integer userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        String jwt = Jwts.builder()
                .setClaims(claims)//自定义内容
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)//签名算法
                .compact();
        return jwt;
    }

    /***
     * 验证 token 的有效性：会同时检查空值、黑名单、签名、格式和过期时间。
     * JJWT 在 parseClaimsJws 阶段会完成签名和 exp 校验，异常统一转成 false。
     * @param token
     * @return
     */
    public boolean checkToken(String token) {
        try {
            if (!StringUtils.hasText(token) || isBlacklisted(token)) {
                return false;
            }
            parseJWT(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 捕获 Token 过期异常
            log.warn("Token 已过期");
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 捕获签名不匹配（密钥错误或数据篡改）
            log.warn("密钥错误或数据篡改");
            return false;
        } catch (MalformedJwtException e) {
            // 捕获 Token 格式错误（如缺少部分、编码问题）
            log.warn("捕获 Token 格式错误");
            return false;
        } catch (UnsupportedJwtException e) {
            // 捕获不支持的 JWT 类型（如算法不匹配）
            log.warn("捕获不支持的 JWT 类型");
            return false;
        } catch (IllegalArgumentException e) {
            // 捕获空 Token 或无效参数
            log.warn("捕获空 Token 或无效参数");
            return false;
        } catch (Exception e) {
            // 兜底处理其他未知异常
            log.warn("其他未知异常", e);
            return false;
        }
    }

    /***
     * 解析 token 拿到 claims。
     * 这里不要吞异常，调用方需要区分 token 过期、签名错误、格式错误等非法状态。
     * @param token
     * @return
     */
    public Claims parseJWT(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /***
     * 查询用户ID
     * @param token
     * @return
     */
    public Integer getUserId(String token) {
        if (StringUtils.hasText(token)) {
            Object userId = parseJWT(token).get("userId");
            if (userId instanceof Integer) {
                return (Integer) userId;
            }
            if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
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
        Integer userId = getUserId(token);
        if (userId == null) {
            return false;
        }
        //查询用户是否存在数据库中
        UserInfo user = userInfoMapper.getUserById(userId);
        return user != null;
    }

    @Override
    public String extractToken(HttpServletRequest request) {
        // 新规范使用 Authorization: Bearer <token>；保留旧 header 兼容，避免一次性切断老前端。
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            authorization = request.getHeader("authorization");
        }
        if (StringUtils.hasText(authorization)) {
            String trimmed = authorization.trim();
            if (trimmed.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                return trimmed.substring(BEARER_PREFIX.length()).trim();
            }
            return trimmed;
        }
        String legacyToken = request.getHeader("token");
        return StringUtils.hasText(legacyToken) ? legacyToken.trim() : null;
    }

    @Override
    public void blacklistToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = parseJWT(token);
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                // 黑名单只保存到 JWT 自身过期时间，避免 Redis 中积累无效 token。
                redisTemplate.opsForValue().set(getBlacklistKey(token), "1", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("加入 JWT 黑名单失败，token 无效或已过期");
        }
    }

    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getBlacklistKey(token)));
    }

    private String getBlacklistKey(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    private SecretKey getSigningKey() {
        // HS256 要求密钥长度至少 256 bit，配置中的 jwt.secret 需要保持 32 字节以上。
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
