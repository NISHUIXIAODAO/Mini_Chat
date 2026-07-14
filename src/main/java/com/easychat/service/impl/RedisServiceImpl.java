package com.easychat.service.impl;

import com.easychat.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Service
@Slf4j
public class RedisServiceImpl implements IRedisService {

    private static final long ONLINE_TTL_SECONDS = 90L;
    private static final String AUTH_SESSION_PREFIX = "auth:session:";
    private static final String WS_ONLINE_PREFIX = "ws:online:";
    private static final String WS_LOCATION_PREFIX = "ws:location:";
    private static final String WS_ACTIVE_PREFIX = "ws:user:active:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /***
     * 生成 Redis Key值 通过用户ID
     * @param userId
     * @param contactType
     * @return
     */
    public String generateRedisKey(Integer userId , Integer contactType){
        if(contactType.equals(CONTACT_TYPE_FRIEND)){
            return "user:" + userId + ":friends";
        } else if (contactType.equals(CONTACT_TYPE_GROUPS)) {
            return "user:" + userId + ":groups";
        }else {
            return null;
        }
    }


    @Override
    public void saveAuthSession(Integer userId, String sessionId, long ttlMillis) {
        if (userId == null || sessionId == null || ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(getAuthSessionKey(userId), sessionId, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isCurrentAuthSession(Integer userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return false;
        }
        Object currentSessionId = redisTemplate.opsForValue().get(getAuthSessionKey(userId));
        return sessionId.equals(currentSessionId);
    }

    @Override
    public void removeAuthSession(Integer userId, String sessionId) {
        if (userId == null || sessionId == null) {
            return;
        }
        Object currentSessionId = redisTemplate.opsForValue().get(getAuthSessionKey(userId));
        if (sessionId.equals(currentSessionId)) {
            redisTemplate.delete(getAuthSessionKey(userId));
        }
    }

    @Override
    public void saveOnlineConnection(Integer userId, String connectionId, String location) {
        if (userId == null || connectionId == null) {
            return;
        }
        redisTemplate.opsForValue().set(getOnlineKey(userId, connectionId), System.currentTimeMillis(), ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(getActiveKey(userId), connectionId, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        if (location != null) {
            redisTemplate.opsForValue().set(getLocationKey(userId, connectionId), location, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void refreshOnlineConnection(Integer userId, String connectionId) {
        if (userId == null || connectionId == null) {
            return;
        }
        redisTemplate.expire(getOnlineKey(userId, connectionId), ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.expire(getLocationKey(userId, connectionId), ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        Object activeConnectionId = redisTemplate.opsForValue().get(getActiveKey(userId));
        if (connectionId.equals(activeConnectionId)) {
            redisTemplate.expire(getActiveKey(userId), ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void removeOnlineConnection(Integer userId, String connectionId) {
        if (userId == null || connectionId == null) {
            return;
        }
        redisTemplate.delete(getOnlineKey(userId, connectionId));
        redisTemplate.delete(getLocationKey(userId, connectionId));
        Object activeConnectionId = redisTemplate.opsForValue().get(getActiveKey(userId));
        if (connectionId.equals(activeConnectionId)) {
            redisTemplate.delete(getActiveKey(userId));
        }
    }

    @Override
    public String getActiveConnectionId(Integer userId) {
        Object connectionId = redisTemplate.opsForValue().get(getActiveKey(userId));
        return connectionId == null ? null : connectionId.toString();
    }

    @Override
    public String getActiveUserLocation(Integer userId) {
        String connectionId = getActiveConnectionId(userId);
        if (connectionId == null) {
            return null;
        }
        Object location = redisTemplate.opsForValue().get(getLocationKey(userId, connectionId));
        return location == null ? null : location.toString();
    }
    /***
     * 校验验证码
     * @param email
     * @return
     */
    public String verifyCode(String email){
        return (String) redisTemplate.opsForValue().get(email);
    }

    /***
     * 存验证码到 redis里
     * @param email
     * @param code
     * @param timeOut
     * @param timeUnit
     */
    public void setCode(String email,String code, long timeOut,TimeUnit timeUnit){
        redisTemplate.opsForValue().set(email, code,timeOut, timeUnit);
    }

    public void addUserContactBatch(String userKey, List<Integer> contactList){
        //清除旧的联系人
        redisTemplate.delete(userKey);
        //将联系人ID列表加入
        if (contactList == null || contactList.isEmpty()) {
            return;
        }
        for (Integer contactId : contactList) {
            redisTemplate.opsForList().rightPush(userKey, contactId);
        }
    }

    public void addUserContact(String userKey , Integer contactId){
        List<Integer> contactList = getUserContactList(userKey);
        if(contactList.contains(contactId)){
            return;
        }
        redisTemplate.opsForList().rightPush(userKey,contactId);

    }

    /***
     * 取联系人列表
     * @param userKey
     * @return
     */
    public List<Integer> getUserContactList(String userKey){
        List<Object> contactList = redisTemplate.opsForList().range(userKey,0,-1);
        List<Integer> result = new ArrayList<>();
        if (contactList == null) {
            return result;
        }
        for (Object contactId : contactList) {
            addContactId(result, contactId);
        }
        return result;
    }

    private void addContactId(List<Integer> result, Object contactId) {
        if (contactId instanceof Integer) {
            result.add((Integer) contactId);
        } else if (contactId instanceof Number) {
            result.add(((Number) contactId).intValue());
        } else if (contactId instanceof String) {
            result.add(Integer.valueOf((String) contactId));
        } else if (contactId instanceof List) {
            for (Object nestedContactId : (List<?>) contactId) {
                addContactId(result, nestedContactId);
            }
        }
    }

    private String getAuthSessionKey(Integer userId) {
        return AUTH_SESSION_PREFIX + userId;
    }

    private String getOnlineKey(Integer userId, String connectionId) {
        return WS_ONLINE_PREFIX + userId + ":" + connectionId;
    }

    private String getLocationKey(Integer userId, String connectionId) {
        return WS_LOCATION_PREFIX + userId + ":" + connectionId;
    }

    private String getActiveKey(Integer userId) {
        return WS_ACTIVE_PREFIX + userId;
    }

}
