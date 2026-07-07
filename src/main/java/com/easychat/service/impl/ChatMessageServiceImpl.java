package com.easychat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.GetMessageHistoryDTO;
import com.easychat.entity.DTO.request.MessageAckDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.DTO.response.MessageHistoryResponseDTO;
import com.easychat.handler.GlobalExceptionHandler;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.service.IChatMessageService;
import com.easychat.service.IJWTService;
import com.easychat.service.application.MessageApplicationService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.SessionIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

/**
 * <p>
 * 聊天消息表 服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Slf4j
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatMessageService {

    private final IJWTService jwtService;
    private final UserContactMapper userContactMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final MessageApplicationService messageApplicationService;

    public ChatMessageServiceImpl(IJWTService jwtService,
                                  UserContactMapper userContactMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  MessageApplicationService messageApplicationService) {
        this.jwtService = jwtService;
        this.userContactMapper = userContactMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.messageApplicationService = messageApplicationService;
    }

    @Override
    public MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        return messageApplicationService.saveMessage(chatSendMessageDTO, request);
    }

    @Override
    public List<MessageHistoryResponseDTO> getMessageHistory(GetMessageHistoryDTO getMessageHistoryDTO,
                                                             HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        Integer userId = jwtService.getUserId(token);

        List<ChatMessage> messageList;
        if (getMessageHistoryDTO.getSessionId() != null && !getMessageHistoryDTO.getSessionId().isEmpty()) {
            messageList = chatMessageMapper.getMessageHistory(
                    getMessageHistoryDTO.getSessionId(),
                    getMessageHistoryDTO.getLastTimestamp(),
                    getMessageHistoryDTO.getLastMessageId(),
                    getMessageHistoryDTO.getForward(),
                    getMessageHistoryDTO.getPageSize()
            );
        } else if (getMessageHistoryDTO.getContactId() != null) {
            Integer contactId = getMessageHistoryDTO.getContactId();
            Integer contactType = userContactMapper.getContactTypeByContactId(userId, contactId);

            if (contactType != null && contactType.equals(CONTACT_TYPE_GROUPS)) {
                messageList = chatMessageMapper.getMessageHistoryByContactId(
                        contactId,
                        getMessageHistoryDTO.getLastTimestamp(),
                        getMessageHistoryDTO.getLastMessageId(),
                        getMessageHistoryDTO.getForward(),
                        getMessageHistoryDTO.getPageSize()
                );
            } else {
                String sessionId = SessionIdUtils.generateSessionId(userId, contactId);
                log.info("GetHistory: userId={}, contactId={}, sessionId={}", userId, contactId, sessionId);
                messageList = chatMessageMapper.getMessageHistory(
                        sessionId,
                        getMessageHistoryDTO.getLastTimestamp(),
                        getMessageHistoryDTO.getLastMessageId(),
                        getMessageHistoryDTO.getForward(),
                        getMessageHistoryDTO.getPageSize()
                );
                log.info("GetHistory Result: count={}", messageList.size());
            }
        } else {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_PARAM_ERROR);
        }

        List<MessageHistoryResponseDTO> resultList = new ArrayList<>();
        for (ChatMessage message : messageList) {
            MessageHistoryResponseDTO dto = new MessageHistoryResponseDTO();
            CopyTools.copyProperties(message, dto);
            resultList.add(dto);
        }

        resultList.sort((o1, o2) -> {
            if (o1.getSendTime() == null || o2.getSendTime() == null) {
                return 0;
            }
            return o1.getSendTime().compareTo(o2.getSendTime());
        });

        return resultList;
    }

    @Override
    public void ackDelivered(MessageAckDTO ackDTO, HttpServletRequest request) {
        messageApplicationService.ackDelivered(ackDTO, request);
    }

    @Override
    public void markRead(MessageAckDTO ackDTO, HttpServletRequest request) {
        messageApplicationService.markRead(ackDTO, request);
    }
}
