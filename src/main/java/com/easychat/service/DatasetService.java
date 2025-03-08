package com.easychat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.easychat.entity.DO.UserInfo;
import com.easychat.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetService {

    private final UserInfoMapper userInfoMapper;

    /***
     * 通过用户ID 判断用户是否存在
     * @return
     */
    public boolean userExists(int userId) {
        UserInfo user = userInfoMapper.getUserById(userId);
        if(user == null){
            log.info("未在数据库中查到");
            return false;
        }
        return true;
    }

    /***
     * 通过email 返回用户类
     * @param email
     * @return
     */
    public UserInfo getUserByEmail(String email) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        QueryWrapper<UserInfo> user = queryWrapper.eq("email", email);
        return userInfoMapper.selectOne(user);
    }
}
