package com.easychat.webSocket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DO.ChatSessionUser;
import com.easychat.entity.DO.UserContactApply;
import com.easychat.entity.DO.UserInfo;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.WsInitDate;
import com.easychat.enums.ContactApplyStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.cache.ContactCacheService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Service
public class WebSocketSessionInitializer {
    private final ContactCacheService contactCacheService;
    private final UserInfoMapper userInfoMapper;
    private final ChatSessionUserMapper chatSessionUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserContactApplyMapper userContactApplyMapper;
    private final LocalChannelRegistry localChannelRegistry;

    public WebSocketSessionInitializer(ContactCacheService contactCacheService,
                                       UserInfoMapper userInfoMapper,
                                       ChatSessionUserMapper chatSessionUserMapper,
                                       ChatMessageMapper chatMessageMapper,
                                       UserContactApplyMapper userContactApplyMapper,
                                       LocalChannelRegistry localChannelRegistry) {
        this.contactCacheService = contactCacheService;
        this.userInfoMapper = userInfoMapper;
        this.chatSessionUserMapper = chatSessionUserMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userContactApplyMapper = userContactApplyMapper;
        this.localChannelRegistry = localChannelRegistry;
    }

    public void initialize(Integer userId) {
        userInfoMapper.updateLastLoginTimeById(userId, LocalDateTime.now());
        UserInfo user = userInfoMapper.getUserById(userId);
        if (user == null) {
            return;
        }
        Long lastOffTime = user.getLastOffTime();
        if (lastOffTime == null || System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000 > lastOffTime) {
            lastOffTime = System.currentTimeMillis();
        }

        List<Integer> sessionIds = contactCacheService.get(userId, CONTACT_TYPE_GROUPS);
        sessionIds.add(userId);
        WsInitDate initData = new WsInitDate();
        List<ChatSessionUser> sessions = chatSessionUserMapper.getSessionListById(userId);
        initData.setChatSessionUserList(sessions);
        List<ChatMessage> messages = chatMessageMapper.getChatMessages(sessionIds, lastOffTime);
        initData.setChatMessagesList(messages);

        LambdaQueryWrapper<UserContactApply> query = new LambdaQueryWrapper<>();
        query.eq(UserContactApply::getReceiveUserId, userId)
                .ge(UserContactApply::getLastApplyTimestamp, lastOffTime)
                .eq(UserContactApply::getStatus, ContactApplyStatusEnum.WAITING.getStatus());
        initData.setApplyCount(Math.toIntExact(userContactApplyMapper.selectCount(query)));

        MessageSendDTO<WsInitDate> initMessage = new MessageSendDTO<>();
        initMessage.setMessageType(MessageTypeEnum.INIT.getType());
        initMessage.setContactId(userId);
        initMessage.setExtendData(initData);
        localChannelRegistry.send(initMessage, userId);
    }
}
