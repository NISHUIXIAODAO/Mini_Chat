package com.easychat.service.application;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.MessageAckDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.handler.GlobalExceptionHandler;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatMessageUserStatusMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IJWTService;
import com.easychat.service.IRedisService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.SessionIdUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

import static com.easychat.enums.MessageStatusEnum.SENDING;
import static com.easychat.enums.MessageStatusEnum.SEND_ED;
import static com.easychat.enums.MessageStatusEnum.DELIVERED;
import static com.easychat.enums.MessageStatusEnum.READ;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;
import static com.easychat.utils.ConstantUtils.ROBOT_ID;
import static com.easychat.utils.ConstantUtils.cleanHtmlTag;

@Slf4j
@Service
public class MessageApplicationService {

    private final IJWTService jwtService;
    private final IRedisService redisService;
    private final UserContactMapper userContactMapper;
    private final UserInfoMapper userInfoMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionUserMapper chatSessionUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageUserStatusMapper chatMessageUserStatusMapper;
    private final MessagePushService messagePushService;
    private final RobotChatService robotChatService;

    public MessageApplicationService(IJWTService jwtService,
                                     IRedisService redisService,
                                     UserContactMapper userContactMapper,
                                     UserInfoMapper userInfoMapper,
                                     ChatSessionMapper chatSessionMapper,
                                     ChatSessionUserMapper chatSessionUserMapper,
                                     ChatMessageMapper chatMessageMapper,
                                     ChatMessageUserStatusMapper chatMessageUserStatusMapper,
                                     MessagePushService messagePushService,
                                     RobotChatService robotChatService) {
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.userContactMapper = userContactMapper;
        this.userInfoMapper = userInfoMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatSessionUserMapper = chatSessionUserMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageUserStatusMapper = chatMessageUserStatusMapper;
        this.messagePushService = messagePushService;
        this.robotChatService = robotChatService;
    }

    @Transactional(rollbackFor = Exception.class)
    public MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        Integer userId = jwtService.getUserId(token);
        Integer contactId = chatSendMessageDTO.getContactId();

        checkContactAllowed(userId, contactId);

        String sendUserNickName = userInfoMapper.getNickNameByUserId(userId);
        String sessionId = SessionIdUtils.generateSessionId(userId, contactId);
        log.info("SaveMessage: userId={}, contactId={}, sessionId={}", userId, contactId, sessionId);

        Integer messageType = chatSendMessageDTO.getMessageType();
        if (messageType == null || (!messageType.equals(MessageTypeEnum.CHAT.getType())
                && !messageType.equals(MessageTypeEnum.MEDIA_CHAT.getType()))) {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_600);
        }

        Integer messageStatus = Objects.equals(MessageTypeEnum.MEDIA_CHAT.getType(), messageType)
                ? SENDING.getStatus()
                : SEND_ED.getStatus();
        String cleanMessageContent = cleanHtmlTag(chatSendMessageDTO.getMessageContent());
        Integer contactType = userContactMapper.getContactTypeByContactId(userId, contactId);
        long curTime = System.currentTimeMillis();

        String lastMessage = cleanMessageContent;
        if (CONTACT_TYPE_GROUPS == contactType) {
            lastMessage = sendUserNickName + ":" + cleanMessageContent;
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId)
                .setMessageType(messageType)
                .setSendUserId(userId)
                .setMessageContent(lastMessage)
                .setSendUserNickName(sendUserNickName)
                .setSendTime(curTime)
                .setContactId(contactId)
                .setContactType(contactType)
                .setFileSize(chatSendMessageDTO.getFileSize())
                .setFileType(chatSendMessageDTO.getFileType())
                .setFileName(chatSendMessageDTO.getFilename())
                .setStatus(messageStatus);
        chatMessageMapper.insert(chatMessage);

        chatSessionMapper.updateBySessionId(sessionId, lastMessage, curTime);
        increaseUnreadForReceivers(userId, contactId, sessionId, contactType);
        createReceiverStatuses(userId, contactId, sessionId, contactType, chatMessage.getMessageId());

        MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
        pushMessageToReceiversAfterCommit(userId, contactId, sessionId, contactType, messageSendDTO);

        if (ROBOT_ID.equals(contactId)) {
            messagePushService.afterCommit(new Runnable() {
                @Override
                public void run() {
                    robotChatService.replyAsync(cleanMessageContent, sessionId, userId);
                }
            });
        }

        return messageSendDTO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void ackDelivered(MessageAckDTO ackDTO, HttpServletRequest request) {
        Long messageId = requireMessageId(ackDTO);
        Integer userId = getCurrentUserId(request);
        long ackTime = System.currentTimeMillis();
        int updated = chatMessageUserStatusMapper.markDelivered(messageId, userId, DELIVERED.getStatus(), ackTime);
        if (updated == 0) {
            chatMessageMapper.markDelivered(messageId, userId, DELIVERED.getStatus(), ackTime);
            return;
        }
        updateAggregateStatus(messageId, ackTime);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(MessageAckDTO ackDTO, HttpServletRequest request) {
        Integer userId = getCurrentUserId(request);
        String sessionId = resolveSessionId(ackDTO, userId);
        Long messageId = ackDTO.getMessageId();
        if (messageId == null) {
            messageId = chatMessageMapper.getMaxMessageIdBySessionId(sessionId);
        }
        if (messageId == null) {
            return;
        }
        long readTime = System.currentTimeMillis();
        int updated = chatMessageUserStatusMapper.markReadBySession(sessionId, userId, messageId, READ.getStatus(), readTime);
        if (updated == 0) {
            chatMessageMapper.markReadBySession(sessionId, userId, messageId, READ.getStatus(), readTime);
        } else {
            chatMessageMapper.updateReadStatusBySessionFromUserStatus(sessionId, messageId, READ.getStatus(), readTime);
        }
        chatSessionUserMapper.markSessionRead(sessionId, userId, messageId, readTime);
    }

    private void createReceiverStatuses(Integer sendUserId, Integer contactId, String sessionId, Integer contactType, Long messageId) {
        if (CONTACT_TYPE_FRIEND == contactType) {
            chatMessageUserStatusMapper.upsertPending(messageId, sessionId, contactId, SEND_ED.getStatus());
            return;
        }
        if (CONTACT_TYPE_GROUPS == contactType) {
            List<Integer> memberIds = chatSessionUserMapper.getUserIdsBySessionId(sessionId);
            for (Integer memberId : memberIds) {
                if (!sendUserId.equals(memberId)) {
                    chatMessageUserStatusMapper.upsertPending(messageId, sessionId, memberId, SEND_ED.getStatus());
                }
            }
        }
    }

    private void updateAggregateStatus(Long messageId, Long statusTime) {
        Integer minStatus = chatMessageUserStatusMapper.getMinStatusByMessageId(messageId);
        if (minStatus == null) {
            return;
        }
        chatMessageMapper.updateStatusAndDeliveredTime(messageId, minStatus, statusTime);
    }

    private void increaseUnreadForReceivers(Integer sendUserId, Integer contactId, String sessionId, Integer contactType) {
        if (CONTACT_TYPE_FRIEND == contactType) {
            chatSessionUserMapper.incrementUnread(sessionId, contactId);
            return;
        }
        if (CONTACT_TYPE_GROUPS == contactType) {
            List<Integer> memberIds = chatSessionUserMapper.getUserIdsBySessionId(sessionId);
            for (Integer memberId : memberIds) {
                if (!sendUserId.equals(memberId)) {
                    chatSessionUserMapper.incrementUnread(sessionId, memberId);
                }
            }
        }
    }

    private void pushMessageToReceiversAfterCommit(Integer sendUserId,
                                                   Integer contactId,
                                                   String sessionId,
                                                   Integer contactType,
                                                   MessageSendDTO<?> messageSendDTO) {
        if (CONTACT_TYPE_FRIEND == contactType) {
            messagePushService.pushToUserAfterCommit(contactId, messageSendDTO);
            return;
        }
        if (CONTACT_TYPE_GROUPS == contactType) {
            List<Integer> memberIds = chatSessionUserMapper.getUserIdsBySessionId(sessionId);
            for (Integer memberId : memberIds) {
                if (!sendUserId.equals(memberId)) {
                    messagePushService.pushToUserAfterCommit(memberId, messageSendDTO);
                }
            }
        }
    }

    private Integer getCurrentUserId(HttpServletRequest request) {
        String token = jwtService.extractToken(request);
        return jwtService.getUserId(token);
    }

    private Long requireMessageId(MessageAckDTO ackDTO) {
        if (ackDTO == null || ackDTO.getMessageId() == null) {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_PARAM_ERROR);
        }
        return ackDTO.getMessageId();
    }

    private String resolveSessionId(MessageAckDTO ackDTO, Integer userId) {
        if (ackDTO == null) {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_PARAM_ERROR);
        }
        if (ackDTO.getSessionId() != null && !ackDTO.getSessionId().isEmpty()) {
            return ackDTO.getSessionId();
        }
        if (ackDTO.getContactId() != null) {
            return SessionIdUtils.generateSessionId(userId, ackDTO.getContactId());
        }
        throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_PARAM_ERROR);
    }

    private void checkContactAllowed(Integer userId, Integer contactId) {
        if (ROBOT_ID.equals(userId)) {
            return;
        }

        List<Integer> friendIdList = redisService.getUserContactList(redisService.generateRedisKey(userId, CONTACT_TYPE_FRIEND));
        List<Integer> groupIdList = redisService.getUserContactList(redisService.generateRedisKey(userId, CONTACT_TYPE_GROUPS));
        if (!friendIdList.contains(contactId) && !groupIdList.contains(contactId)) {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_UNEXIST);
        }

        if (friendIdList.contains(contactId)) {
            Integer status = userContactMapper.getStatusByUserIdAndContactId(userId, contactId);
            if (FriendStatusEnum.FRIEND_BLACK.getCode().equals(status)) {
                throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_BLACK);
            }
        }
    }
}
