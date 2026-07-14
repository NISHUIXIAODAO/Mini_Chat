package com.easychat.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface IRedisService {

    public String generateRedisKey(Integer userId , Integer contactType);
    public void saveAuthSession(Integer userId, String sessionId, long ttlMillis);
    public boolean isCurrentAuthSession(Integer userId, String sessionId);
    public void removeAuthSession(Integer userId, String sessionId);
    public void saveOnlineConnection(Integer userId, String connectionId, String location);
    public void refreshOnlineConnection(Integer userId, String connectionId);
    public void removeOnlineConnection(Integer userId, String connectionId);
    public String getActiveConnectionId(Integer userId);
    public String getActiveUserLocation(Integer userId);
    public String verifyCode(String email);
    public void setCode(String email, String code, long timeOut, TimeUnit timeUnit);
    public void addUserContactBatch(String userKey, List<Integer> contactList);
    public void addUserContact(String userKey , Integer contactId);
    public List<Integer> getUserContactList(String userKey);
}
