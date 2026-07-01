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
import com.easychat.webSocket.ChannelContextUtils;
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

import cn.hutool.http.HttpRequest;
import java.net.InetAddress;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private ChannelContextUtils channelContextUtils;

    @Value("${server.port:5050}")
    private String serverPort;

    private String localIp;

    public ChatMessageServiceImpl() {
        try {
            localIp = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            localIp = "127.0.0.1";
        }
    }

    @Override
    public MessageSendDTO saveMessage(ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response) {
        String token = jwtService.extractToken(request);
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
        logger.info("SaveMessage: userId={}, contactId={}, sessionId={}", sendUserId, contactId, sessionId);


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
        
        // 改造：使用精准推送代替 Kafka 广播
        // kafkaMessageProducer.sendMessage(messageSendDTO);
        pushMessageToUser(contactId, messageSendDTO);

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
        String token = jwtService.extractToken(request);
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
            
            // 如果是群聊(1)，按contactId查询；否则（单聊0或null/陌生人），按sessionId查询
            if (contactType != null && contactType.equals(CONTACT_TYPE_GROUPS)) { // 群聊
                messageList = chatMessageMapper.getMessageHistoryByContactId(
                    contactId,
                    getMessageHistoryDTO.getLastTimestamp(),
                    getMessageHistoryDTO.getPageSize()
                );
            } else { // 单聊 (好友或陌生人)
                String sessionId = SessionIdUtils.generateSessionId(userId, contactId);
                logger.info("GetHistory: userId={}, contactId={}, sessionId={}", userId, contactId, sessionId);
                messageList = chatMessageMapper.getMessageHistory(
                    sessionId,
                    getMessageHistoryDTO.getLastTimestamp(),
                    getMessageHistoryDTO.getPageSize()
                );
                logger.info("GetHistory Result: count={}", messageList.size());
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
        
        // 按时间升序排序（旧消息在前，新消息在后），方便前端展示
        resultList.sort((o1, o2) -> {
            if (o1.getSendTime() == null || o2.getSendTime() == null) {
                return 0;
            }
            return o1.getSendTime().compareTo(o2.getSendTime());
        });
        
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

                // 5. 推送消息 (精准推送)
                MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
                // kafkaMessageProducer.sendMessage(messageSendDTO);
                pushMessageToUser(userContactId, messageSendDTO);

            } catch (Exception e) {
                logger.error("机器人回复失败", e);
            }
        });
    }

    /**
     * 精准推送消息
     */
    private void pushMessageToUser(Integer userId, MessageSendDTO message) {
        try {
            // 1. 从 Redis 获取用户所在的服务器地址 (ip:port)
            String targetAddress = redisService.getUserLocation(userId);
            
            logger.info("准备推送消息给用户: {}, Redis记录地址: {}, 本机地址: {}:{}", userId, targetAddress, localIp, serverPort);
            
            if (targetAddress == null) {
                // 用户不在线，无需推送
                logger.info("用户 {} 不在线 (Redis无位置记录)", userId);
                return;
            }

            // 解析 IP 和 端口
            String[] parts = targetAddress.split(":");
            String targetIp = parts[0];
            String targetPort = parts.length > 1 ? parts[1] : "5050";

            // 2. 判断是否为本机
            // 必须同时匹配 IP 和 端口
            boolean isLocalIp = targetIp.equals(localIp) || targetIp.equals("127.0.0.1") || targetIp.equals("localhost");
            boolean isLocalPort = targetPort.equals(serverPort);

            if (isLocalIp && isLocalPort) {
                // 是本机，直接推送
                logger.info("目标用户在【本机】，直接通过WebSocket推送");
                channelContextUtils.sendMessage(message);
            } else {
                // 3. 如果是其他机器，调用内部 HTTP 接口推送
                String url = "http://" + targetIp + ":" + targetPort + "/internal/push";
                logger.info("目标用户在【远程节点】({}:{})，发起HTTP转发: {}", targetIp, targetPort, url);
                
                CompletableFuture.runAsync(() -> {
                    try {
                        HttpRequest.post(url)
                                .body(cn.hutool.json.JSONUtil.toJsonStr(message))
                                .timeout(2000) // 2秒超时
                                .execute();
                    } catch (Exception e) {
                        logger.error("HTTP转发消息失败", e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("消息推送失败", e);
        }
    }
}
