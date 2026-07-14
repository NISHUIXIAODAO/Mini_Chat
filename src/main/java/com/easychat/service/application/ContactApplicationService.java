package com.easychat.service.application;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DO.ChatSession;
import com.easychat.entity.DO.GroupInfo;
import com.easychat.entity.DO.UserContact;
import com.easychat.entity.DO.UserContactApply;
import com.easychat.entity.DO.UserInfo;
import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.handler.GlobalExceptionHandler;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.GroupInfoMapper;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.mapper.UserContactMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IJWTService;
import com.easychat.service.IRedisService;
import com.easychat.service.domain.SessionDomainService;
import com.easychat.utils.CopyTools;
import com.easychat.webSocket.ChannelContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.easychat.enums.ContactApplyStatusEnum.AGREE;
import static com.easychat.enums.ContactApplyStatusEnum.BLACK;
import static com.easychat.enums.ContactApplyStatusEnum.WAITING;
import static com.easychat.enums.FriendStatusEnum.FRIEND_BLACK;
import static com.easychat.enums.FriendStatusEnum.FRIEND_YES;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;
import static com.easychat.utils.SessionIdUtils.generateGroupSessionId;
import static com.easychat.utils.SessionIdUtils.generatePrivateSessionId;

@Slf4j
@Service
public class ContactApplicationService {

    private final UserContactMapper userContactMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserContactApplyMapper userContactApplyMapper;
    private final IJWTService jwtService;
    private final IRedisService redisService;
    private final GroupInfoMapper groupInfoMapper;
    private final ChannelContextUtils channelContextUtils;
    private final SessionDomainService sessionDomainService;
    private final MessagePushService messagePushService;

    public ContactApplicationService(UserContactMapper userContactMapper,
                                     ChatSessionMapper chatSessionMapper,
                                     ChatMessageMapper chatMessageMapper,
                                     UserInfoMapper userInfoMapper,
                                     UserContactApplyMapper userContactApplyMapper,
                                     IJWTService jwtService,
                                     IRedisService redisService,
                                     GroupInfoMapper groupInfoMapper,
                                     ChannelContextUtils channelContextUtils,
                                     SessionDomainService sessionDomainService,
                                     MessagePushService messagePushService) {
        this.userContactMapper = userContactMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userInfoMapper = userInfoMapper;
        this.userContactApplyMapper = userContactApplyMapper;
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.groupInfoMapper = groupInfoMapper;
        this.channelContextUtils = channelContextUtils;
        this.sessionDomainService = sessionDomainService;
        this.messagePushService = messagePushService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultVo<Object> applyFriendAdd(String token, Integer contactId, String applyInfo) {
        try {
            UserInfo contactUser = userInfoMapper.getUserById(contactId);
            if (contactUser == null) {
                throw new GlobalExceptionHandler.BusinessException("联系人不存在");
            }

            Integer applyUserId = jwtService.getUserId(token);
            Long curTime = System.currentTimeMillis();
            Integer receiveUserId = contactId;
            Integer receiveJoinType = userInfoMapper.getUserJoinType(contactId);

            UserContact receiveContact = userContactMapper.getReceiveInfo(receiveUserId, applyUserId);
            Integer status = FriendStatusEnum.FRIEND_NO.getCode();
            if (receiveContact != null) {
                status = receiveContact.getStatus();
            }
            if (receiveContact != null && status.equals(FRIEND_BLACK.getCode())) {
                throw new GlobalExceptionHandler.BusinessException("对方已把你拉黑");
            }

            UserContactApply applyRecode = userContactApplyMapper
                    .getByApplyUserIdAddReceiveUserIdAddContactId(applyUserId, receiveUserId, contactId);

            if (receiveJoinType == 0) {
                UserContact applyContact = userContactMapper.getByUserIdAndContactId(applyUserId, receiveUserId);
                if (isFriend(applyContact) && isFriend(receiveContact)) {
                    return ResultVo.success("已是好友");
                }
                ChatMessage chatMessage = agreeFriendContact(applyUserId, receiveUserId, applyInfo);
                log.info("无需同意，添加成功");
                messagePushService.afterCommit(new Runnable() {
                    @Override
                    public void run() {
                        syncFriendContactCache(applyUserId, receiveUserId);
                        sendFriendAgreeMessage(chatMessage, applyUserId, receiveUserId);
                    }
                });
            } else {
                userContactApplyMapper.upsertApply(applyUserId, receiveUserId, CONTACT_TYPE_FRIEND, contactId,
                        curTime, WAITING.getStatus(), applyInfo);
                log.info(applyRecode == null ? "已发起好友申请" : "已重新发起好友申请");
            }

            if (applyRecode == null || !MessageTypeEnum.INIT.getType().equals(applyRecode.getContactType())) {
                MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
                messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
                messageSendDTO.setMessageContent(applyInfo);
                messageSendDTO.setContactId(receiveUserId);
                messagePushService.pushToUserAfterCommit(receiveUserId, messageSendDTO);
            }

            return ResultVo.success("申请好友成功");
        } catch (Exception e) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return ResultVo.failed("applyAddError" + e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultVo<Object> applyGroupAdd(ApplyGroupAddDTO applyGroupAddDTO, HttpServletRequest request) {
        Integer groupId = applyGroupAddDTO.getContactId();
        GroupInfo groupInfo = groupInfoMapper.getByGroupId(groupId);
        if (groupInfo == null) {
            throw new GlobalExceptionHandler.BusinessException("群聊不存在");
        }

        String applyUserToken = jwtService.extractToken(request);
        Integer applyUserId = jwtService.getUserId(applyUserToken);
        Long curTime = System.currentTimeMillis();
        Integer groupOwnerId = groupInfoMapper.getOwnerIdByGroupId(groupId);
        Integer groupJoinType = groupInfoMapper.getJoinTypeByGroupId(groupId);

        UserContactApply applyRecode = userContactApplyMapper
                .getByApplyUserIdAddReceiveUserIdAddContactId(applyUserId, groupOwnerId, groupId);

        if (groupJoinType == 0) {
            UserContact existContact = userContactMapper.getByUserIdAndContactId(applyUserId, groupId);
            if (isFriend(existContact)) {
                return ResultVo.success("已加入群聊");
            }
            saveOrUpdateContact(applyUserId, groupId, CONTACT_TYPE_GROUPS, FRIEND_YES.getCode());
            agreeGroupContact(applyUserId, groupId);
            log.info("无需同意，添加成功");
        } else {
            userContactApplyMapper.upsertApply(applyUserId, groupOwnerId, CONTACT_TYPE_GROUPS, groupId,
                    curTime, WAITING.getStatus(), applyGroupAddDTO.getApplyInfo());
            log.info(applyRecode == null ? "已发起加入群聊申请" : "已重新发起加入群聊申请");
        }

        if (applyRecode == null) {
            MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
            messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
            messageSendDTO.setMessageContent(applyGroupAddDTO.getApplyInfo());
            messageSendDTO.setContactId(groupOwnerId);
            messagePushService.pushToUserAfterCommit(groupOwnerId, messageSendDTO);
        }

        return ResultVo.success("申请加入群聊成功");
    }

    public ResultVo<Object> getContactList(String token) {
        Integer userId = jwtService.getUserId(token);

        try {
            List<Integer> friendIdList = userContactMapper.getListById(userId, CONTACT_TYPE_FRIEND);
            List<Integer> groupIdList = userContactMapper.getListById(userId, CONTACT_TYPE_GROUPS);
            List<Map<String, Object>> contactList = new ArrayList<>();

            for (Integer friendId : friendIdList) {
                UserInfo friendInfo = userInfoMapper.getUserById(friendId);
                if (friendInfo != null) {
                    String sessionId = generatePrivateSessionId(userId, friendId);
                    ChatSession chatSession = chatSessionMapper.getBySessionId(sessionId);

                    Map<String, Object> contactMap = new HashMap<>();
                    contactMap.put("contactId", friendId);
                    contactMap.put("nickName", friendInfo.getNickName());
                    contactMap.put("contactType", CONTACT_TYPE_FRIEND);
                    contactMap.put("lastMessage", chatSession != null ? chatSession.getLastMessage() : "");
                    contactMap.put("lastTime", chatSession != null ? chatSession.getLastReceiveTime() : 0L);
                    contactList.add(contactMap);
                }
            }

            for (Integer groupId : groupIdList) {
                GroupInfo groupInfo = groupInfoMapper.getByGroupId(groupId);
                if (groupInfo != null) {
                    String sessionId = generateGroupSessionId(groupId);
                    ChatSession chatSession = chatSessionMapper.getBySessionId(sessionId);

                    Map<String, Object> contactMap = new HashMap<>();
                    contactMap.put("contactId", groupId);
                    contactMap.put("nickName", groupInfo.getGroupName());
                    contactMap.put("contactType", CONTACT_TYPE_GROUPS);
                    contactMap.put("lastMessage", chatSession != null ? chatSession.getLastMessage() : "");
                    contactMap.put("lastTime", chatSession != null ? chatSession.getLastReceiveTime() : 0L);
                    contactList.add(contactMap);
                }
            }

            contactList.sort((c1, c2) -> {
                Long time1 = (Long) c1.get("lastTime");
                Long time2 = (Long) c2.get("lastTime");
                return time2.compareTo(time1);
            });

            return ResultVo.success(contactList);
        } catch (Exception e) {
            log.error("获取联系人列表错误：{}", e.getMessage(), e);
            return ResultVo.failed("获取联系人列表失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultVo<Object> disposeApply(DisposeApplyDTO disposeApplyDTO, HttpServletRequest request) {
        Integer applyUserId = disposeApplyDTO.getApplyUserId();
        Integer status = disposeApplyDTO.getStatus();
        String token = jwtService.extractToken(request);

        Integer receiveUserId = jwtService.getUserId(token);
        UserContactApply applyRecord = getApplyRecord(disposeApplyDTO, applyUserId, receiveUserId);
        if (applyRecord == null) {
            return ResultVo.failed("未收到好友申请");
        }

        if (AGREE.getStatus().equals(applyRecord.getStatus())) {
            return ResultVo.success("申请已同意");
        }
        if (!WAITING.getStatus().equals(applyRecord.getStatus())) {
            return ResultVo.success("申请已处理");
        }

        int updateCount = userContactApplyMapper.updateStatusByApplyIdAndStatus(
                applyRecord.getApplyId(), WAITING.getStatus(), status);
        if (updateCount == 0) {
            UserContactApply latestApplyRecord = userContactApplyMapper.getByApplyId(applyRecord.getApplyId());
            if (latestApplyRecord != null && AGREE.getStatus().equals(latestApplyRecord.getStatus())) {
                return ResultVo.success("申请已同意");
            }
            return ResultVo.success("申请已处理");
        }

        Integer contactType = applyRecord.getContactType();
        if (Integer.valueOf(CONTACT_TYPE_FRIEND).equals(contactType)) {
            if (status.equals(AGREE.getStatus())) {
                ChatMessage chatMessage = agreeFriendContact(applyUserId, receiveUserId, applyRecord.getApplyInfo());
                messagePushService.afterCommit(new Runnable() {
                    @Override
                    public void run() {
                        syncFriendContactCache(applyUserId, receiveUserId);
                        sendFriendAgreeMessage(chatMessage, applyUserId, receiveUserId);
                    }
                });
            } else if (status.equals(BLACK.getStatus())) {
                saveOrUpdateContact(applyUserId, receiveUserId, contactType, FRIEND_BLACK.getCode());
            }
        } else if (Integer.valueOf(CONTACT_TYPE_GROUPS).equals(contactType)) {
            Integer groupId = applyRecord.getContactId();
            if (status.equals(AGREE.getStatus())) {
                saveOrUpdateContact(applyUserId, groupId, contactType, FRIEND_YES.getCode());
                agreeGroupContact(applyUserId, groupId);
            } else if (status.equals(BLACK.getStatus())) {
                saveOrUpdateContact(applyUserId, groupId, contactType, FRIEND_BLACK.getCode());
            }
        }

        return ResultVo.success("处理好友申请成功");
    }

    private void agreeGroupContact(final Integer applyUserId, final Integer groupId) {
        String sessionId = generateGroupSessionId(groupId);
        final GroupInfo groupInfo = groupInfoMapper.getByGroupId(groupId);
        sessionDomainService.saveOrUpdateSessionUser(applyUserId, groupId, sessionId, groupInfo.getGroupName());

        UserInfo applyUserInfo = userInfoMapper.getUserById(applyUserId);
        String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(), applyUserInfo.getNickName());
        sessionDomainService.saveOrUpdateSession(sessionId, sendMessage, System.currentTimeMillis());

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
        chatMessage.setMessageContent(sendMessage);
        chatMessage.setSendTime(System.currentTimeMillis());
        chatMessage.setContactId(groupId);
        chatMessage.setContactType(CONTACT_TYPE_GROUPS);
        chatMessage.setStatus(MessageStatusEnum.SEND_ED.getStatus());
        chatMessageMapper.insert(chatMessage);

        final MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
        messageSendDTO.setContactId(groupId);
        Integer groupMemberCount = userContactMapper.getGroupCountByContactIdAndStatus(groupId, FRIEND_YES.getCode());
        messageSendDTO.setMemberCount(groupMemberCount);
        messageSendDTO.setContactName(groupInfo.getGroupName());
        messagePushService.afterCommit(new Runnable() {
            @Override
            public void run() {
                redisService.addUserContact(redisService.generateRedisKey(applyUserId, CONTACT_TYPE_GROUPS), groupInfo.getGroupId());
                messagePushService.pushToUser(applyUserId, messageSendDTO);
            }
        });
    }

    private ChatMessage agreeFriendContact(Integer applyUserId, Integer receiveUserId, String applyInfo) {
        saveOrUpdateContact(applyUserId, receiveUserId, CONTACT_TYPE_FRIEND, FRIEND_YES.getCode());
        saveOrUpdateContact(receiveUserId, applyUserId, CONTACT_TYPE_FRIEND, FRIEND_YES.getCode());

        String sessionId = generatePrivateSessionId(applyUserId, receiveUserId);
        sessionDomainService.saveOrUpdateSession(sessionId, applyInfo, System.currentTimeMillis());

        UserInfo receiveUser = userInfoMapper.getUserById(receiveUserId);
        UserInfo applyUser = userInfoMapper.getUserById(applyUserId);
        sessionDomainService.saveOrUpdateSessionUser(applyUserId, receiveUserId, sessionId, receiveUser.getNickName());
        sessionDomainService.saveOrUpdateSessionUser(receiveUserId, applyUserId, sessionId, applyUser.getNickName());

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
        chatMessage.setMessageContent(applyInfo);
        chatMessage.setSendUserId(applyUserId);
        chatMessage.setSendUserNickName(applyUser.getNickName());
        chatMessage.setContactId(receiveUserId);
        chatMessage.setSendTime(System.currentTimeMillis());
        chatMessage.setContactType(CONTACT_TYPE_FRIEND);
        chatMessage.setStatus(MessageStatusEnum.SEND_ED.getStatus());
        chatMessageMapper.insert(chatMessage);
        return chatMessage;
    }

    private void saveOrUpdateContact(Integer userId, Integer contactId, int contactType, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        userContactMapper.upsertContact(userId, contactId, contactType, now, status, now);
    }

    private boolean isFriend(UserContact userContact) {
        return userContact != null && FRIEND_YES.getCode().equals(userContact.getStatus());
    }

    private UserContactApply getApplyRecord(DisposeApplyDTO disposeApplyDTO, Integer applyUserId, Integer receiveUserId) {
        if (disposeApplyDTO.getApplyId() != null) {
            UserContactApply applyRecord = userContactApplyMapper.getByApplyId(disposeApplyDTO.getApplyId());
            if (applyRecord != null
                    && receiveUserId.equals(applyRecord.getReceiveUserId())
                    && (applyUserId == null || applyUserId.equals(applyRecord.getApplyUserId()))) {
                return applyRecord;
            }
            return null;
        }

        if (applyUserId == null) {
            return null;
        }

        if (disposeApplyDTO.getContactId() != null && disposeApplyDTO.getContactType() != null) {
            return userContactApplyMapper.getByApplyUserIdAndReceiveUserIdAndContactIdAndContactType(
                    applyUserId,
                    receiveUserId,
                    disposeApplyDTO.getContactId(),
                    disposeApplyDTO.getContactType());
        }

        UserContactApply waitingApplyRecord = userContactApplyMapper.getLatestByApplyUserIdAndReceiveUserIdAndStatus(
                applyUserId, receiveUserId, WAITING.getStatus());
        if (waitingApplyRecord != null) {
            return waitingApplyRecord;
        }
        return userContactApplyMapper.getLatestByApplyUserIdAndReceiveUserId(applyUserId, receiveUserId);
    }

    private void syncFriendContactCache(Integer applyUserId, Integer receiveUserId) {
        String receiveUserKey = redisService.generateRedisKey(receiveUserId, CONTACT_TYPE_FRIEND);
        String applyUserKey = redisService.generateRedisKey(applyUserId, CONTACT_TYPE_FRIEND);
        redisService.addUserContact(receiveUserKey, applyUserId);
        redisService.addUserContact(applyUserKey, receiveUserId);
    }

    private void sendFriendAgreeMessage(ChatMessage chatMessage, Integer applyUserId, Integer receiveUserId) {
        UserInfo receiveUser = userInfoMapper.getUserById(receiveUserId);
        MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
        messagePushService.pushToUser(receiveUserId, messageSendDTO);

        MessageSendDTO selfMessageSendDTO = CopyTools.copy(chatMessage);
        selfMessageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
        selfMessageSendDTO.setContactId(applyUserId);
        selfMessageSendDTO.setExtendData(receiveUser);
        messagePushService.pushToUser(applyUserId, selfMessageSendDTO);
    }
}
