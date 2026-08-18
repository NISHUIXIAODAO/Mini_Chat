package com.easychat.service.auth;

import com.easychat.entity.DTO.response.WebSocketTicketResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketConnectionTicketService {
    private static final String KEY_PREFIX = "ws:ticket:";
    private static final int TICKET_BYTES = 32;
    private static final int TICKET_LENGTH = 43;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthSessionService authSessionService;

    @Value("${ws.ticket-ttl-seconds:60}")
    private long ticketTtlSeconds;

    public WebSocketConnectionTicketService(RedisTemplate<String, Object> redisTemplate,
                                            AuthSessionService authSessionService) {
        this.redisTemplate = redisTemplate;
        this.authSessionService = authSessionService;
    }

    public WebSocketTicketResponseDTO issue(Integer userId, String sessionId) {
        if (userId == null || sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Authenticated session is required");
        }
        String ticket = newTicket();
        redisTemplate.opsForValue().set(key(ticket), userId + "|" + sessionId,
                ticketTtlSeconds, TimeUnit.SECONDS);
        return new WebSocketTicketResponseDTO(ticket, TimeUnit.SECONDS.toMillis(ticketTtlSeconds));
    }

    /**
     * Atomically consumes a connection ticket and returns its user only while the login session remains current.
     */
    public Integer consume(String ticket) {
        if (!isValidTicketFormat(ticket)) {
            return null;
        }
        Object value = redisTemplate.opsForValue().getAndDelete(key(ticket));
        if (!(value instanceof String)) {
            return null;
        }
        String[] parts = ((String) value).split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            Integer userId = Integer.valueOf(parts[0]);
            return authSessionService.isCurrent(userId, parts[1]) ? userId : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String newTicket() {
        byte[] bytes = new byte[TICKET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isValidTicketFormat(String ticket) {
        return ticket != null && ticket.length() == TICKET_LENGTH && ticket.matches("[A-Za-z0-9_-]+");
    }

    private String key(String ticket) {
        return KEY_PREFIX + sha256(ticket);
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
