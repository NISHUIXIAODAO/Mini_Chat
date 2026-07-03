package com.easychat.service.application;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.handler.GlobalExceptionHandler;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
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
    private final ChatMessageMapper chatMessageMapper;
    private final MessagePushService messagePushService;
    private final RobotChatService robotChatService;

    public MessageApplicationService(IJWTService jwtService,
                                     IRedisService redisService,
                                     UserContactMapper userContactMapper,
                                     UserInfoMapper userInfoMapper,
                                     ChatSessionMapper chatSessionMapper,
                                     ChatMessageMapper chatMessageMapper,
                                     MessagePushService messagePushService,
                                     RobotChatService robotChatService) {
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.userContactMapper = userContactMapper;
        this.userInfoMapper = userInfoMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
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
        chatSessionMapper.updateBySessionId(sessionId, lastMessage, curTime);

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

        MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
        messagePushService.pushToUserAfterCommit(contactId, messageSendDTO);

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
