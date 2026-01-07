package com.easychat.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface IRedisService {

    public String generateRedisKey(Integer userId , Integer contactType);
    public Long getUserHeartBeat(Integer userId);
    public void saveHeartBeat(Integer userId);
    public void removeUserHeartBeat(Integer userId);
    public String verifyCode(String email);
    public void setCode(String email, String code, long timeOut, TimeUnit timeUnit);
    public void addUserContactBatch(String userKey, List<Integer> contactList);
    public void addUserContact(String userKey , Integer contactId);
    public List<Integer> getUserContactList(String userKey);
    public void saveUserLocation(Integer userId, String ip);
    public String getUserLocation(Integer userId);
    public void removeUserLocation(Integer userId);
}
