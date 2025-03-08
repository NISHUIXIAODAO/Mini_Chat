package com.easychat.service.impl;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

}
