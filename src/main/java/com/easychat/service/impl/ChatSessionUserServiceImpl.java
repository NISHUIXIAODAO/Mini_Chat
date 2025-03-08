package com.easychat.service.impl;

import com.easychat.entity.DO.ChatSessionUser;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.service.IChatSessionUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话用户表 服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Service
public class ChatSessionUserServiceImpl extends ServiceImpl<ChatSessionUserMapper, ChatSessionUser> implements IChatSessionUserService {

}
