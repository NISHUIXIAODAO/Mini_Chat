package com.easychat.service.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Service
public class ContactCacheService {
    private final RedisTemplate<String, Object> redisTemplate;

    public ContactCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String keyFor(Integer userId, Integer contactType) {
        if (CONTACT_TYPE_FRIEND == contactType) { return "user:" + userId + ":friends"; }
        if (CONTACT_TYPE_GROUPS == contactType) { return "user:" + userId + ":groups"; }
        throw new IllegalArgumentException("Unsupported contact type: " + contactType);
    }

    public void replace(Integer userId, Integer contactType, List<Integer> contacts) {
        String key = keyFor(userId, contactType);
        redisTemplate.delete(key);
        if (contacts != null) {
            for (Integer contact : contacts) {
                redisTemplate.opsForList().rightPush(key, contact);
            }
        }
    }

    public void add(Integer userId, Integer contactType, Integer contactId) {
        String key = keyFor(userId, contactType);
        if (!get(userId, contactType).contains(contactId)) {
            redisTemplate.opsForList().rightPush(key, contactId);
        }
    }

    public List<Integer> get(Integer userId, Integer contactType) {
        List<Object> values = redisTemplate.opsForList().range(keyFor(userId, contactType), 0, -1);
        List<Integer> contacts = new ArrayList<>();
        if (values == null) { return contacts; }
        for (Object value : values) {
            if (value instanceof Number) { contacts.add(((Number) value).intValue()); }
            else if (value != null) { contacts.add(Integer.valueOf(value.toString())); }
        }
        return contacts;
    }
}
