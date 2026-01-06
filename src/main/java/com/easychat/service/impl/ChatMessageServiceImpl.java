package com.easychat.service.impl;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.GetMessageHistoryDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.DTO.response.MessageHistoryResponseDTO;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.hander.GlobalExceptionHandler;
import com.easychat.kafka.KafkaMessageProducer;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.AIService;
import com.easychat.service.IChatMessageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.service.IJWTService;
import com.easychat.service.IRedisService;
import com.easychat.utils.CopyTools;
import com.easychat.utils.SessionIdUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

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
    private KafkaMessageProducer kafkaMessageProducer;
    @Autowired
    private AIService aiService;

    @Override
    public MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response) {
        String token = request.getHeader("authorization");
        Integer userId = jwtService.getUserId(token);
        String sendUserNickName = userInfoMapper.getNickNameByUserId(userId);
        Integer contactId = chatSendMessageDTO.getContactId();

        //判断是否是机器人回复，判断好友状态
        if (!userId.equals(ROBOT_ID)) {
            List<Integer> friendIdList = redisService.getUserContactList(redisService.generateRedisKey(userId, CONTACT_TYPE_FRIEND));
            List<Integer> groupIdList = redisService.getUserContactList(redisService.generateRedisKey(userId, CONTACT_TYPE_GROUPS));
            //判断是否存在好友或群组
            if (!friendIdList.contains(contactId) && !groupIdList.contains(contactId)) {
                throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_UNEXIST);
            } else if (friendIdList.contains(contactId)) {
                //判断是否拉黑
                Integer status = userContactMapper.getStatusByUserIdAndContactId(userId, contactId);
                if (status.equals(FriendStatusEnum.FRIEND_BLACK.getCode())) {
                    throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_BLACK);
                }
            }

        }
        String sessionId = null;
        Integer sendUserId = userId;

        long curTime = System.currentTimeMillis();
        //查询联系人是单聊还是群聊
        Integer contactType = userContactMapper.getContactTypeByContactId(userId, contactId);

        //单聊 else 群聊
        sessionId = SessionIdUtils.generateSessionId(sendUserId, contactId);


        Integer messageType = chatSendMessageDTO.getMessageType();
        //若是chat和media_chat之外的消息，不发
        if (messageType == null || (!messageType.equals(MessageTypeEnum.CHAT.getType()) && !messageType.equals(MessageTypeEnum.MEDIA_CHAT.getType()))) {
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_600);
        }

        Integer messageStatus = Objects.equals(MessageTypeEnum.MEDIA_CHAT.getType(), messageType) ? SENDING.getStatus() : SEND_ED.getStatus();

        // 脚本转义，消息清洗
        String messageContent = cleanHtmlTag(chatSendMessageDTO.getMessageContent());

        //更新会话表
        String lastMessage = messageContent;
        if (contactType.equals(CONTACT_TYPE_GROUPS)) {
            lastMessage = sendUserNickName + ":" + messageContent;
        }
        chatSessionMapper.updateBySessionId(sessionId, lastMessage, curTime);

        ChatMessage chatMessage = new ChatMessage();
        //更新消息表
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
        kafkaMessageProducer.sendMessage(messageSendDTO);

    // 机器人自动回复
        if (ROBOT_ID.equals(contactId)) {
            // 注意：userId 可能是 null，如果 jwtService 解析失败，但前面已经用过了应该没事。
            // 重点检查 chatWithRobot 方法内部是否使用了可能为 null 的对象
            chatWithRobot(messageContent, sessionId, userId);
        }

        return messageSendDTO;

    }
    
    @Override
    public List<MessageHistoryResponseDTO> getMessageHistory(GetMessageHistoryDTO getMessageHistoryDTO, HttpServletRequest request) {
        String token = request.getHeader("token");
        Integer userId = jwtService.getUserId(token);
        
        List<ChatMessage> messageList;
        if (getMessageHistoryDTO.getSessionId() != null && !getMessageHistoryDTO.getSessionId().isEmpty()) {
            // 根据会话ID查询
            messageList = chatMessageMapper.getMessageHistory(
                getMessageHistoryDTO.getSessionId(),
                getMessageHistoryDTO.getLastTimestamp(),
                getMessageHistoryDTO.getPageSize()
            );
        } else if (getMessageHistoryDTO.getContactId() != null) {
            // 根据联系人ID查询
            // 如果是单聊，需要生成sessionId
            Integer contactId = getMessageHistoryDTO.getContactId();
            Integer contactType = userContactMapper.getContactTypeByContactId(userId, contactId);
            
            if (contactType != null && contactType == 0) { // 单聊
                String sessionId = SessionIdUtils.generateSessionId(userId, contactId);
                messageList = chatMessageMapper.getMessageHistory(
                    sessionId,
                    getMessageHistoryDTO.getLastTimestamp(),
                    getMessageHistoryDTO.getPageSize()
                );
            } else { // 群聊或其他情况，直接按contactId查询
                messageList = chatMessageMapper.getMessageHistoryByContactId(
                    contactId,
                    getMessageHistoryDTO.getLastTimestamp(),
                    getMessageHistoryDTO.getPageSize()
                );
            }
        } else {
            // 参数不足
            throw new GlobalExceptionHandler.BusinessException(GlobalExceptionHandler.ErrorCode.CODE_PARAM_ERROR);
        }
        
        // 转换为响应DTO
        List<MessageHistoryResponseDTO> resultList = new ArrayList<>();
        for (ChatMessage message : messageList) {
            MessageHistoryResponseDTO dto = new MessageHistoryResponseDTO();
            CopyTools.copyProperties(message, dto);
            resultList.add(dto);
        }
        
        return resultList;
    }

    /**
     * 机器人自动回复逻辑 (异步执行)
     */
    private void chatWithRobot(String content, String sessionId, Integer userContactId) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 调用 AI 接口获取回复
                String aiReply = aiService.chat(content);

                // 2. 构造机器人回复的消息对象
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setSessionId(sessionId);
                chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
                chatMessage.setSendUserId(ROBOT_ID); // 发送者是机器人
                chatMessage.setSendUserNickName(ROBOT_NAME);
                chatMessage.setMessageContent(aiReply);
                chatMessage.setSendTime(System.currentTimeMillis());
                chatMessage.setContactId(userContactId); // 接收者是用户
                chatMessage.setContactType(0); // 单聊
                chatMessage.setStatus(SEND_ED.getStatus());

                // 3. 保存消息到数据库
                chatMessageMapper.insert(chatMessage);

                // 4. 更新会话表的最后一条消息
                chatSessionMapper.updateBySessionId(sessionId, aiReply, System.currentTimeMillis());

                // 5. 发送 Kafka 消息 (推送到前端)
                MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
                kafkaMessageProducer.sendMessage(messageSendDTO);

            } catch (Exception e) {
                logger.error("机器人回复失败", e);
            }
        });
    }
}
