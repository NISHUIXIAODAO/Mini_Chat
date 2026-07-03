package com.easychat.service.application;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.service.AIService;
import com.easychat.utils.CopyTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;

import static com.easychat.enums.MessageStatusEnum.SEND_ED;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.ROBOT_ID;
import static com.easychat.utils.ConstantUtils.ROBOT_NAME;

@Slf4j
@Service
public class RobotChatService {

    private final AIService aiService;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final TransactionTemplate transactionTemplate;
    private final MessagePushService messagePushService;

    public RobotChatService(AIService aiService,
                            ChatMessageMapper chatMessageMapper,
                            ChatSessionMapper chatSessionMapper,
                            TransactionTemplate transactionTemplate,
                            MessagePushService messagePushService) {
        this.aiService = aiService;
        this.chatMessageMapper = chatMessageMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.transactionTemplate = transactionTemplate;
        this.messagePushService = messagePushService;
    }

    public void replyAsync(final String content, final String sessionId, final Integer userContactId) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final String aiReply = aiService.chat(content);
                    MessageSendDTO messageSendDTO = transactionTemplate.execute(status -> {
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setSessionId(sessionId);
                        chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
                        chatMessage.setSendUserId(ROBOT_ID);
                        chatMessage.setSendUserNickName(ROBOT_NAME);
                        chatMessage.setMessageContent(aiReply);
                        chatMessage.setSendTime(System.currentTimeMillis());
                        chatMessage.setContactId(userContactId);
                        chatMessage.setContactType(CONTACT_TYPE_FRIEND);
                        chatMessage.setStatus(SEND_ED.getStatus());

                        chatMessageMapper.insert(chatMessage);
                        chatSessionMapper.updateBySessionId(sessionId, aiReply, System.currentTimeMillis());
                        return CopyTools.copy(chatMessage);
                    });
                    messagePushService.pushToUser(userContactId, messageSendDTO);
                } catch (Exception e) {
                    log.error("机器人回复失败", e);
                }
            }
        });
    }
}
