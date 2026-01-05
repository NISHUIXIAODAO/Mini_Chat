package com.easychat.service;

import com.easychat.entity.DO.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.GetMessageHistoryDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.DTO.response.MessageHistoryResponseDTO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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
    
    /**
     * 获取聊天历史记录
     * @param getMessageHistoryDTO 请求参数
     * @param request HTTP请求
     * @return 聊天历史记录列表
     */
    List<MessageHistoryResponseDTO> getMessageHistory(GetMessageHistoryDTO getMessageHistoryDTO, HttpServletRequest request);
}
