package com.easychat.service.impl;

import com.easychat.entity.DO.ChatSession;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.service.IChatSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会话信息 服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements IChatSessionService {

}
