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
import static com.easychat.utils.SessionIdUtils.generateSessionId;

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
                ChatMessage chatMessage = agreeFriendContact(applyUserId, receiveUserId, applyInfo);
                log.info("无需同意，添加成功");
                messagePushService.afterCommit(new Runnable() {
                    @Override
                    public void run() {
                        syncFriendContactCache(applyUserId, receiveUserId);
                        sendFriendAgreeMessage(chatMessage, applyUserId, receiveUserId);
                    }
                });
            } else if (applyRecode == null) {
                UserContactApply contactApply = new UserContactApply();
                contactApply.setApplyUserId(applyUserId);
                contactApply.setReceiveUserId(receiveUserId);
                contactApply.setContactType(CONTACT_TYPE_FRIEND);
                contactApply.setLastApplyTime(curTime);
                contactApply.setContactId(contactId);
                contactApply.setStatus(WAITING.getStatus());
                contactApply.setApplyInfo(applyInfo);
                userContactApplyMapper.insert(contactApply);
                log.info("已发起好友申请");
            } else {
                userContactApplyMapper.updateByApplyId(applyRecode.getApplyId(), WAITING.getStatus(), curTime, applyInfo);
                log.info("已重新发起好友申请");
            }

            if (applyRecode == null || !MessageTypeEnum.INIT.getType().equals(applyRecode.getContactType())) {
                MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
                messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
                messageSendDTO.setMessageContent(applyInfo);
                messageSendDTO.setContactId(receiveUserId);
                messagePushService.sendKafkaAfterCommit(messageSendDTO);
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
            saveOrUpdateContact(applyUserId, groupId, CONTACT_TYPE_GROUPS, FRIEND_YES.getCode());
            log.info("无需同意，添加成功");
        } else if (applyRecode == null) {
            UserContactApply contactApply = new UserContactApply();
            contactApply.setApplyUserId(applyUserId);
            contactApply.setReceiveUserId(groupOwnerId);
            contactApply.setContactType(CONTACT_TYPE_GROUPS);
            contactApply.setLastApplyTime(curTime);
            contactApply.setContactId(groupId);
            contactApply.setStatus(WAITING.getStatus());
            contactApply.setApplyInfo(applyGroupAddDTO.getApplyInfo());
            userContactApplyMapper.insert(contactApply);
            log.info("已发起加入群聊申请");
        } else {
            userContactApplyMapper.updateByApplyId(applyRecode.getApplyId(),
                    WAITING.getStatus(),
                    curTime,
                    applyGroupAddDTO.getApplyInfo());
            log.info("已重新发起加入群聊申请");
        }

        if (applyRecode == null) {
            MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
            messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
            messageSendDTO.setMessageContent(applyGroupAddDTO.getApplyInfo());
            messageSendDTO.setContactId(groupOwnerId);
            messagePushService.sendKafkaAfterCommit(messageSendDTO);
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
                    String sessionId = generateSessionId(userId, friendId);
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
                    String sessionId = generateSessionId(userId, groupId);
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
        Integer groupId = groupInfoMapper.getGroupIdByOwnerId(receiveUserId);
        String applyInfo = userContactApplyMapper.getApplyInfoByApplyUserIdAndReceiveUserId(applyUserId, receiveUserId);
        Integer contactType = userContactApplyMapper.getContactTypeByApplyUserIdAndReceiveUserId(applyUserId, receiveUserId);
        if (contactType == null) {
            return ResultVo.failed("未收到好友申请");
        }

        userContactApplyMapper.setStatus(applyUserId, receiveUserId, status);
        if (contactType == CONTACT_TYPE_FRIEND) {
            if (status.equals(AGREE.getStatus())) {
                ChatMessage chatMessage = agreeFriendContact(applyUserId, receiveUserId, applyInfo);
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
        } else if (contactType == CONTACT_TYPE_GROUPS) {
            if (status.equals(AGREE.getStatus())) {
                saveOrUpdateContact(applyUserId, groupId, contactType, FRIEND_YES.getCode());
            } else if (status.equals(BLACK.getStatus())) {
                saveOrUpdateContact(applyUserId, groupId, contactType, FRIEND_BLACK.getCode());
            }
        }

        if (contactType == CONTACT_TYPE_GROUPS) {
            agreeGroupContact(applyUserId, groupId);
        }

        return ResultVo.success("处理好友申请成功");
    }

    private void agreeGroupContact(final Integer applyUserId, final Integer groupId) {
        String sessionId = generateSessionId(applyUserId, groupId);
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
                channelContextUtils.addUser2Group(applyUserId, groupInfo.getGroupId());
                messagePushService.sendKafka(messageSendDTO);
            }
        });
    }

    private ChatMessage agreeFriendContact(Integer applyUserId, Integer receiveUserId, String applyInfo) {
        saveOrUpdateContact(applyUserId, receiveUserId, CONTACT_TYPE_FRIEND, FRIEND_YES.getCode());
        saveOrUpdateContact(receiveUserId, applyUserId, CONTACT_TYPE_FRIEND, FRIEND_YES.getCode());

        String sessionId = generateSessionId(applyUserId, receiveUserId);
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
        UserContact existContact = userContactMapper.getByUserIdAndContactId(userId, contactId);
        if (existContact == null) {
            userContactMapper.insertContact(userId, contactId, contactType, now, status, now);
            return;
        }
        userContactMapper.updateContact(userId, contactId, contactType, status, now);
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
        messagePushService.sendKafka(messageSendDTO);

        MessageSendDTO selfMessageSendDTO = CopyTools.copy(chatMessage);
        selfMessageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
        selfMessageSendDTO.setContactId(applyUserId);
        selfMessageSendDTO.setExtendData(receiveUser);
        messagePushService.sendKafka(selfMessageSendDTO);
    }
}
