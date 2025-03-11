package com.easychat.service.impl;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.hander.GlobalExceptionHandler;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.service.IJWTService;
import com.easychat.service.IRedisService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.SessionIdUtils;
import com.easychat.webSocket.MessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Objects;

import static com.easychat.enums.MessageStatusEnum.SENDING;
import static com.easychat.enums.MessageStatusEnum.SEND_ED;
import static com.easychat.utils.ConstantUtils.*;

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

    @Autowired
    private IJWTService jwtService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private UserContactMapper userContactMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private MessageHandler messageHandler;

    @Override
    public MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader("token");
        Integer userId = jwtService.getUserId(token);
        String sendUserNickName = userInfoMapper.getNickNameByUserId(userId);
        Integer contactId = chatSendMessageDTO.getContactId();

        ChatMessage chatMessage = new ChatMessage();

        //判断是否是机器人回复，判断好友状态
        if(!userId.equals(ROBOT_ID)){
            List<Integer> friendIdList = redisService.getUserContactList(redisService.generateRedisKey(userId,CONTACT_TYPE_FRIEND));
            List<Integer> groupIdList = redisService.getUserContactList(redisService.generateRedisKey(userId,CONTACT_TYPE_GROUPS));
            //判断是否存在好友或群组
            if(!friendIdList.contains(contactId) && !groupIdList.contains(contactId) ){
                //判断是否拉黑
                Integer status = userContactMapper.getStatusByUserIdAndContactId(userId,contactId);
                if(status.equals(FriendStatusEnum.FRIEND_BLACK.getCode())){
                    throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_BLACK);
                }
            }
        }
        String sessionId = null;
        Integer sendUserId = userId;

        Long curTime = System.currentTimeMillis();
        //查询联系人是单聊还是群聊
        Integer contactType = userContactMapper.getContactTypeByContactId(contactId);

        //单聊 else 群聊
        sessionId = SessionIdUtils.generateSessionId(sendUserId,contactId);


        Integer messageType = chatSendMessageDTO.getMessageType();
        //若是chat和media_chat之外的消息，不发
        if(messageType == null || (!messageType.equals(MessageTypeEnum.CHAT.getType()) && !messageType.equals(MessageTypeEnum.MEDIA_CHAT.getType()))){
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_600);
        }

        Integer messageStatus = Objects.equals(MessageTypeEnum.MEDIA_CHAT.getType(), messageType) ? SENDING.getStatus() : SEND_ED.getStatus();

        String messageContent = cleanHtmlTag(chatSendMessageDTO.getMessageContent());

        //更新会话
        String lastMessage = messageContent;
        if(contactType.equals(CONTACT_TYPE_GROUPS)){
            lastMessage = sendUserNickName + ":" + messageContent;
        }
        chatSessionMapper.updateBySessionId(sessionId,lastMessage,curTime);

        //更新会话表
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(messageType);
        chatMessage.setSendUserId(userId);
        chatMessage.setMessageContent(lastMessage);
        chatMessage.setSendUserNickName(sendUserNickName);
        chatMessage.setSendTime(curTime);
        chatMessage.setContactId(contactId);
        chatMessage.setContactType(contactType);
        chatMessage.setFileSize(chatSendMessageDTO.getFileSize());
        chatMessage.setFileType(chatSendMessageDTO.getFileType());
        chatMessage.setFileName(chatSendMessageDTO.getFilename());
        chatMessage.setStatus(messageStatus);
        chatMessageMapper.insert(chatMessage);

        MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
        messageHandler.sendMessage(messageSendDTO);

        return messageSendDTO;

    }
}
