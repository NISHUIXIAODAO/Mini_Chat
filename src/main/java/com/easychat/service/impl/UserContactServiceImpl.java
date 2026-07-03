package com.easychat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DO.ChatSession;
import com.easychat.entity.DO.ChatSessionUser;
import com.easychat.entity.DO.UserContact;
import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.ResultVo;
import com.easychat.enums.MessageStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.service.IJWTService;
import com.easychat.service.IUserContactService;
import com.easychat.service.application.ContactApplicationService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static com.easychat.enums.FriendStatusEnum.FRIEND_YES;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.ROBOT_ID;
import static com.easychat.utils.ConstantUtils.ROBOT_MESSAGE;
import static com.easychat.utils.ConstantUtils.ROBOT_NAME;
import static com.easychat.utils.SessionIdUtils.generateSessionId;

/**
 * <p>
 * 添加联系人
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
@Service
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact> implements IUserContactService {

    private final UserContactMapper userContactMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionUserMapper chatSessionUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final IJWTService jwtService;
    private final ContactApplicationService contactApplicationService;

    public UserContactServiceImpl(UserContactMapper userContactMapper,
                                  ChatSessionMapper chatSessionMapper,
                                  ChatSessionUserMapper chatSessionUserMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  IJWTService jwtService,
                                  ContactApplicationService contactApplicationService) {
        this.userContactMapper = userContactMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatSessionUserMapper = chatSessionUserMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.jwtService = jwtService;
        this.contactApplicationService = contactApplicationService;
    }

    @Override
    public List<Integer> getFriendIdList(Integer userId) {
        return userContactMapper.getListById(userId, CONTACT_TYPE_FRIEND);
    }

    @Override
    public List<Integer> getGroupIdList(Integer userId) {
        return userContactMapper.getListById(userId, com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS);
    }

    @Override
    public void addContact4Robot(Integer userId) {
        UserContact userContact = new UserContact();
        userContact.setUserId(userId)
                .setContactId(ROBOT_ID)
                .setContactType(CONTACT_TYPE_FRIEND)
                .setCreateTime(LocalDateTime.now())
                .setLastUpdateTime(LocalDateTime.now())
                .setStatus(FRIEND_YES.getCode());
        userContactMapper.insert(userContact);

        userContact.setUserId(ROBOT_ID)
                .setContactId(userId)
                .setContactType(CONTACT_TYPE_FRIEND)
                .setCreateTime(LocalDateTime.now())
                .setLastUpdateTime(LocalDateTime.now())
                .setStatus(FRIEND_YES.getCode());
        userContactMapper.insert(userContact);

        Date curDate = new Date();
        String sessionId = generateSessionId(1, userId);
        ChatSession chatSession = new ChatSession()
                .setSessionId(sessionId)
                .setLastReceiveTime(curDate.getTime())
                .setLastMessage(ROBOT_MESSAGE);
        chatSessionMapper.insert(chatSession);

        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(userId)
                .setContactId(ROBOT_ID)
                .setContactName(ROBOT_NAME)
                .setSessionId(sessionId);
        chatSessionUserMapper.insert(chatSessionUser);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId)
                .setMessageType(MessageTypeEnum.CHAT.getType())
                .setMessageContent(ROBOT_MESSAGE)
                .setSendUserId(ROBOT_ID)
                .setSendUserNickName(ROBOT_NAME)
                .setSendTime(curDate.getTime())
                .setContactId(userId)
                .setContactType(CONTACT_TYPE_FRIEND)
                .setStatus(MessageStatusEnum.SEND_ED.getStatus());
        chatMessageMapper.insert(chatMessage);
    }

    @Override
    public ResultVo<Object> applyFriendAdd(String token, Integer contactId, String applyInfo) {
        return contactApplicationService.applyFriendAdd(token, contactId, applyInfo);
    }

    @Override
    public ResultVo<Object> applyGroupAdd(ApplyGroupAddDTO applyGroupAddDTO,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        return contactApplicationService.applyGroupAdd(applyGroupAddDTO, request);
    }

    @Override
    public ResultVo<Object> getContactList(HttpServletRequest request, HttpServletResponse response) {
        return contactApplicationService.getContactList(jwtService.extractToken(request));
    }

    @Override
    public ResultVo<Object> disposeApply(DisposeApplyDTO disposeApplyDTO,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        return contactApplicationService.disposeApply(disposeApplyDTO, request);
    }
}
