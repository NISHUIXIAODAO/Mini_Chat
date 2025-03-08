package com.easychat.service.impl;

import com.easychat.entity.DO.UserContactApply;
import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.service.IUserContactApplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 联系人申请 服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-03
 */


@Service
public class UserContactApplyServiceImpl extends ServiceImpl<UserContactApplyMapper, UserContactApply> implements IUserContactApplyService {

    @Autowired
    private JWTServiceImpl jwtService;
    @Autowired
    private UserContactApplyMapper userContactApplyMapper;

    /***
     * 获取好友申请列表
     * @param token
     * @return
     */
    public List<UserApplyListResponseDTO> getApplyList(String token){
        Integer receiveUserId = jwtService.getUserId(token);
        return userContactApplyMapper.getApplyList(receiveUserId);
    }
}
