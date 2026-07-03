package com.easychat.service.impl;

import com.easychat.entity.DO.UserContactApply;
import com.easychat.entity.DTO.response.UserApplyListResponseDTO;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.service.IJWTService;
import com.easychat.service.IUserContactApplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

    private final IJWTService jwtService;
    private final UserContactApplyMapper userContactApplyMapper;

    public UserContactApplyServiceImpl(IJWTService jwtService, UserContactApplyMapper userContactApplyMapper) {
        this.jwtService = jwtService;
        this.userContactApplyMapper = userContactApplyMapper;
    }

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
