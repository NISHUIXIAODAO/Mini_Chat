package com.easychat.service.impl;

import com.easychat.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Service
@Slf4j
public class RedisServiceImpl implements IRedisService {

    @Autowired
    private RedisTemplate redisTemplate;

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


    /***
     * 获取用户心跳
     * @param userId
     * @return
     */
    public Long getUserHeartBeat(Integer userId){
        return (Long) redisTemplate.opsForValue().get("heartBeat" + userId);
    }

    /***
     * 保存用户心跳
     * @param userId
     */
    public void saveHeartBeat(Integer userId){
        redisTemplate.opsForValue().set("heartBeat" + userId, System.currentTimeMillis(),11);
    }

    /***
     * 移除用户心跳
     * @param userId
     */
    public void removeUserHeartBeat(Integer userId){
        redisTemplate.delete("heartBeat" + userId);
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
        redisTemplate.opsForList().rightPushAll(userKey,contactList);
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
        return redisTemplate.opsForList().range(userKey,0,-1);
    }

    @Override
    public void saveUserLocation(Integer userId, String ip) {
        redisTemplate.opsForValue().set("user:location:" + userId, ip, 1, TimeUnit.DAYS);
    }

    @Override
    public String getUserLocation(Integer userId) {
        return (String) redisTemplate.opsForValue().get("user:location:" + userId);
    }

    @Override
    public void removeUserLocation(Integer userId) {
        redisTemplate.delete("user:location:" + userId);
    }

}
