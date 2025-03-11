package com.easychat.service;

import com.easychat.entity.DO.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 聊天消息表 服务类
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
public interface IChatMessageService extends IService<ChatMessage> {

    MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response);
}
