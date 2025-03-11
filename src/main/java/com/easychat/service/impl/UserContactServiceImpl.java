package com.easychat.service.impl;

import com.easychat.entity.*;
import com.easychat.entity.DO.*;
import com.easychat.entity.DTO.request.ApplyGroupAddDTO;
import com.easychat.entity.DTO.request.DisposeApplyDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.FriendStatusEnum;
import com.easychat.enums.MessageStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.hander.GlobalExceptionHandler;
import com.easychat.mapper.*;
import com.easychat.service.IJWTService;
import com.easychat.service.IRedisService;
import com.easychat.service.IUserContactService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.utils.CopyTools;
import com.easychat.webSocket.ChannelContextUtils;
import com.easychat.webSocket.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static com.easychat.enums.ContactApplyStatusEnum.*;
import static com.easychat.enums.FriendStatusEnum.FRIEND_BLACK;
import static com.easychat.enums.FriendStatusEnum.FRIEND_YES;
import static com.easychat.utils.ConstantUtils.*;
import static com.easychat.utils.SessionIdUtils.generateSessionId;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author my
 * @since 2025-03-01
 */
@Slf4j
@Service
public class UserContactServiceImpl extends ServiceImpl<UserContactMapper, UserContact> implements IUserContactService {

    @Autowired
    private UserContactMapper userContactMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private ChatSessionUserMapper chatSessionUserMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private IJWTService jwtService;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserContactApplyMapper userContactApplyMapper;
    @Autowired
    private MessageHandler messageHandler;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private GroupInfoMapper groupInfoMapper;
    @Autowired
    private ChannelContextUtils channelContextUtils;


    //查询联系人(朋友)
    public List<Integer> getFriendIdList(Integer userId){
        return userContactMapper.getListById(userId,CONTACT_TYPE_FRIEND);
    }
    //查询群聊
    public List<Integer> getGroupIdList(Integer userId){
        return userContactMapper.getListById(userId,CONTACT_TYPE_GROUPS);
    }


    /***
     * 添加机器人好友
     * @param userId
     */
    @Override
    public void addContact4Robot(Integer userId) {

        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(ROBOT_ID);
        userContact.setContactType(CONTACT_TYPE_FRIEND);
        userContact.setCreateTime(LocalDateTime.now());
        userContact.setLastUpdateTime(LocalDateTime.now());
        userContact.setStatus(FRIEND_YES.getCode());
        userContactMapper.insert(userContact);

        //增加会话信息
        Date curDate = new Date();
        String sessionId = generateSessionId(1,userId);
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastReceiveTime(curDate.getTime());
        chatSession.setLastMessage(ROBOT_MESSAGE);
        chatSessionMapper.insert(chatSession);

        //增加会话人信息
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(userId);
        chatSessionUser.setContactId(ROBOT_ID);
        chatSessionUser.setContactName(ROBOT_NAME);
        chatSessionUser.setSessionId(sessionId);
        chatSessionUserMapper.insert(chatSessionUser);

        //增加聊天消息
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setMessageType(MessageTypeEnum.CHAT.getType());
        chatMessage.setMessageContent(ROBOT_MESSAGE);
        chatMessage.setSendUserId(ROBOT_ID);
        chatMessage.setSendUserNickName(ROBOT_NAME);
        chatMessage.setSendTime(curDate.getTime());
        chatMessage.setContactId(userId);
        chatMessage.setContactType(CONTACT_TYPE_FRIEND);
        chatMessage.setStatus(MessageStatusEnum.SEND_ED.getStatus());
        chatMessageMapper.insert(chatMessage);
    }

    /**
     * 发送好友申请
     * @param token
     * @param contactId
     * @param applyInfo
     * @return
     */
    @Override
    public ResultVo<Object> applyFriendAdd(String token, Integer contactId, String applyInfo){
        try{
            UserInfo contactUser = userInfoMapper.getUserById(contactId);
            if(contactUser== null){
                throw new GlobalExceptionHandler.BusinessException("联系人不存在");
            }
            //申请人ID (通过token拿到ID)
            Integer applyUserId = jwtService.getUserId(token);

            //默认申请信息 todo


            Long curTime = System.currentTimeMillis();
            Integer receiveUserId = contactId;
            //查被添加人的join_type
            Integer receiveJoinType = userInfoMapper.getUserJoinType(contactId);

            //查询是否已添加，或者被拉黑
            UserContact receiveContact = userContactMapper.getReceiveInfo(receiveUserId,applyUserId);
            Integer status = FriendStatusEnum.FRIEND_NO.getCode();
            if(receiveContact != null){
                status = receiveContact.getStatus();
            }
            if(receiveContact != null && status.equals(FriendStatusEnum.FRIEND_BLACK.getCode())){
                throw new GlobalExceptionHandler.BusinessException("对方已把你拉黑");
            }

            //查询是否有申请记录
            UserContactApply applyRecode = userContactApplyMapper.getByApplyUserIdAddReceiveUserIdAddContactId(applyUserId,receiveUserId,contactId);

            //被加好友的user的join_type是无需同意直接加好友
            if(receiveJoinType == 0){
                UserContact userContact = new UserContact();
                userContact.setUserId(applyUserId);
                userContact.setContactId(receiveUserId);
                userContact.setContactType(CONTACT_TYPE_FRIEND);
                if(receiveContact == null){
                    userContact.setCreateTime(LocalDateTime.now());
                }
                userContact.setStatus(FRIEND_YES.getCode());
                userContact.setLastUpdateTime(LocalDateTime.now());
                userContactMapper.insert(userContact);
                log.info("无需同意，添加成功");
            } else {
                //receiveUser的join_type是需要同意才可以加好友
                //查询是否有申请记录
                if(applyRecode == null){
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
                }else {
                    //更新申请记录
                    userContactApplyMapper.updateByApplyId(applyRecode.getApplyId(),
                            WAITING.getStatus(),
                            curTime,
                            applyInfo);
                    log.info("已重新发起好友申请");
                }
            }

            //发送WebSocket消息通知
            if(applyRecode == null || !MessageTypeEnum.INIT.getType().equals(applyRecode.getContactType())){
                MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
                messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
                messageSendDTO.setMessageContent(applyInfo);
                messageSendDTO.setContactId(receiveUserId);
                messageHandler.sendMessage(messageSendDTO);
            }

            return ResultVo.success("申请好友成功");
        }catch (Exception e){
            return ResultVo.failed("applyAddError" + e);
        }

    }

    @Override
    public ResultVo<Object> applyGroupAdd(ApplyGroupAddDTO applyGroupAddDTO, HttpServletRequest request, HttpServletResponse response) {
        Integer groupId = applyGroupAddDTO.getContactId();
        GroupInfo groupInfo = groupInfoMapper.getByGroupId(groupId);
        if(groupInfo == null){
            throw new GlobalExceptionHandler.BusinessException("群聊不存在");
        }
        String applyUserToken = request.getHeader("token");
        //申请人ID (通过token拿到ID)
        Integer applyUserId = jwtService.getUserId(applyUserToken);
        Long curTime = System.currentTimeMillis();

        //查群主ID，将申请信息发向群主
        Integer groupOwnerId = groupInfoMapper.getOwnerIdByGroupId(groupId);
        //查群组的join_type
        Integer groupJoinType = groupInfoMapper.getJoinTypeByGroupId(groupId);

        //查询是否有申请记录
        UserContactApply applyRecode = userContactApplyMapper.getByApplyUserIdAddReceiveUserIdAddContactId(applyUserId,groupOwnerId,groupId);

        //查询是否已入群
        UserContact receiveContact = userContactMapper.getReceiveInfo(applyUserId,groupJoinType);
        //group的join_type是 不需要 群主同意就可以进群
        if(groupJoinType == 0){
            UserContact userContact = new UserContact();
            userContact.setUserId(applyUserId);
            userContact.setContactId(groupId);
            userContact.setContactType(CONTACT_TYPE_GROUPS);
            if(receiveContact == null){
                userContact.setCreateTime(LocalDateTime.now());
            }
            userContact.setStatus(FRIEND_YES.getCode());
            userContact.setLastUpdateTime(LocalDateTime.now());
            userContactMapper.insert(userContact);
            log.info("无需同意，添加成功");
        } else {
            //group的join_type是需要群主同意才可以进群
            //查询是否有申请记录
            if(applyRecode == null){
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
            }else {
                //更新申请记录
                userContactApplyMapper.updateByApplyId(applyRecode.getApplyId(),
                        WAITING.getStatus(),
                        curTime,
                        applyGroupAddDTO.getApplyInfo());
                log.info("已重新发起加入群聊申请");
            }
        }

        //发送WebSocket消息通知
        if(applyRecode == null){
            MessageSendDTO<Object> messageSendDTO = new MessageSendDTO<Object>();
            messageSendDTO.setMessageType(MessageTypeEnum.CONTACT_APPLY.getType());
            messageSendDTO.setMessageContent(applyGroupAddDTO.getApplyInfo());
            messageSendDTO.setContactId(groupOwnerId);
            messageHandler.sendMessage(messageSendDTO);
        }

        return ResultVo.success("申请加入群聊成功");

    }

    /***
     * 处理单条好友申请
     * @param disposeApplyDTO
     * @param request
     * @param response
     * @return
     */
    public ResultVo<Object> disposeApply(DisposeApplyDTO disposeApplyDTO , HttpServletRequest request,HttpServletResponse response){
        Integer applyUserId = disposeApplyDTO.getApplyUserId();
        Integer status = disposeApplyDTO.getStatus();
        String token = request.getHeader("token");

        Integer receiveUserId = jwtService.getUserId(token);
        Integer groupId = groupInfoMapper.getGroupIdByOwnerId(receiveUserId);
        String applyInfo = userContactApplyMapper.getApplyInfoByApplyUserIdAndReceiveUserId(applyUserId,receiveUserId);
        Integer contactType = userContactApplyMapper.getContactTypeByApplyUserIdAndReceiveUserId(applyUserId,receiveUserId);
        if(contactType == null){
            return ResultVo.failed("未收到好友申请");
        }
        //处理 添加好友/群聊 的类型 的申请
        userContactApplyMapper.setStatus(applyUserId,receiveUserId,status);
        //被请求者 同意 请求者
        if(contactType == CONTACT_TYPE_FRIEND){
            if (status.equals(AGREE.getStatus())){
                userContactMapper.insertContact(
                        applyUserId,
                        receiveUserId,
                        contactType,
                        LocalDateTime.now(),
                        FRIEND_YES.getCode(),
                        LocalDateTime.now());
                userContactMapper.insertContact(
                        receiveUserId,
                        applyUserId,
                        contactType,
                        LocalDateTime.now(),
                        FRIEND_YES.getCode(),
                        LocalDateTime.now());

                //同意好友申请后需要将新好友放入Redis中friendIdList中
                String receiveUserKey = redisService.generateRedisKey(receiveUserId,CONTACT_TYPE_FRIEND);
                String applyUserKey = redisService.generateRedisKey(applyUserId,CONTACT_TYPE_FRIEND);
                redisService.addUserContact(receiveUserKey,applyUserId);
                redisService.addUserContact(applyUserKey,receiveUserId);
            }
            //被请求者 拉黑 请求者
            else if (status.equals(BLACK.getStatus())){
                userContactMapper.insertContact(
                        applyUserId,
                        receiveUserId,
                        contactType,
                        LocalDateTime.now(),
                        FRIEND_BLACK.getCode(),
                        LocalDateTime.now());
            }
        } else if (contactType == CONTACT_TYPE_GROUPS) {
            if (status.equals(AGREE.getStatus())) {
                userContactMapper.insertContact(
                        applyUserId,
                        groupId,
                        contactType,
                        LocalDateTime.now(),
                        FRIEND_YES.getCode(),
                        LocalDateTime.now());
                //同意好友申请后需要将新群聊放入Redis中GroupIdList中
                String groupKey = redisService.generateRedisKey(groupId, CONTACT_TYPE_GROUPS);
                String applyUserKey = redisService.generateRedisKey(applyUserId, CONTACT_TYPE_GROUPS);
                redisService.addUserContact(groupKey, applyUserId);
                redisService.addUserContact(applyUserKey, groupId);
                //被请求者 拉黑 请求者
            } else if (status.equals(BLACK.getStatus())){
                userContactMapper.insertContact(
                        applyUserId,
                        groupId,
                        contactType,
                        LocalDateTime.now(),
                        FRIEND_BLACK.getCode(),
                        LocalDateTime.now());
            }
        }
        //创建会话
        String sessionId = null;
        //创建单聊sessionId
        if(contactType == CONTACT_TYPE_FRIEND){
            sessionId = generateSessionId(applyUserId,receiveUserId);
        }
        //创建群聊sessionId
        if(contactType == CONTACT_TYPE_GROUPS){
            sessionId = generateSessionId(applyUserId,groupId);
        }

        if(contactType == CONTACT_TYPE_FRIEND){
            //创建会话
            if(chatSessionMapper.boolSessionId(sessionId) != null){
                //已存在会话，更新会话
                chatSessionMapper.updateBySessionId(sessionId,applyInfo,System.currentTimeMillis());
            }
            //会话不存在，创建会话
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionId(sessionId);
            chatSession.setLastMessage(applyInfo);
            chatSession.setLastReceiveTime(System.currentTimeMillis());
            chatSessionMapper.insert(chatSession);

            //申请人Session
            UserInfo receiveUser = userInfoMapper.getByUserId(receiveUserId);
            if(chatSessionUserMapper.boolByUserIdAndContactId(applyUserId,receiveUserId) != null){
                chatSessionUserMapper.updateByUserIdAndContactId(applyUserId,receiveUserId,receiveUser.getNickName());
            }
            ChatSessionUser applySessionUser = new ChatSessionUser();
            applySessionUser.setUserId(applyUserId);
            applySessionUser.setContactId(receiveUserId);
            applySessionUser.setSessionId(sessionId);
            applySessionUser.setContactName(receiveUser.getNickName());
            chatSessionUserMapper.insert(applySessionUser);


            //接受人Session
            UserInfo applyUser = userInfoMapper.getByUserId(applyUserId);
            if(chatSessionUserMapper.boolByUserIdAndContactId(receiveUserId,applyUserId) != null){
                chatSessionUserMapper.updateByUserIdAndContactId(receiveUserId,applyUserId,applyUser.getNickName());
            }
            ChatSessionUser receiveSessionUser = new ChatSessionUser();
            receiveSessionUser.setUserId(receiveUserId);
            receiveSessionUser.setContactId(applyUserId);
            receiveSessionUser.setSessionId(sessionId);
            receiveSessionUser.setContactName(applyUser.getNickName());
            chatSessionUserMapper.insert(receiveSessionUser);

            //记录消息表
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            chatMessage.setMessageContent(applyInfo);
            chatMessage.setSendUserId(applyUserId);
            chatMessage.setSendUserNickName(applyUser.getNickName());
            chatMessage.setContactId(receiveUserId);
            chatMessage.setSendTime(System.currentTimeMillis());
            chatMessage.setContactType(CONTACT_TYPE_FRIEND);
            chatMessageMapper.insert(chatMessage);

            MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
            //发送给receive用户
            messageHandler.sendMessage(messageSendDTO);

            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND_SELF.getType());
            messageSendDTO.setContactId(applyUserId);
            messageSendDTO.setExtendData(receiveUser);

            messageHandler.sendMessage(messageSendDTO);

        } else {
            //加入群组
            ChatSessionUser chatSessionUser = new ChatSessionUser();
            chatSessionUser.setUserId(applyUserId);
            chatSessionUser.setContactId(groupId);    //GroupId
            GroupInfo groupInfo = groupInfoMapper.getByGroupId(groupId);
            chatSessionUser.setContactName(groupInfo.getGroupName());
            chatSessionUser.setSessionId(sessionId);
            chatSessionUserMapper.insert(chatSessionUser);

            UserInfo applyUserInfo = userInfoMapper.getByUserId(applyUserId);
            String sendMessage = String.format(MessageTypeEnum.ADD_GROUP.getInitMessage(),applyUserInfo.getNickName());
            //增加Session信息
            if(chatSessionMapper.boolSessionId(sessionId) != null){
                chatSessionMapper.updateBySessionId(sessionId,sendMessage,System.currentTimeMillis());
            } else {
                ChatSession chatSession = new ChatSession();
                chatSession.setSessionId(sessionId);
                chatSession.setLastMessage(sendMessage);
                chatSession.setLastReceiveTime(System.currentTimeMillis());
                chatSessionMapper.insert(chatSession);
            }

            //增加聊天消息
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(sessionId);
            chatMessage.setMessageType(MessageTypeEnum.ADD_GROUP.getType());
            chatMessage.setMessageContent(sendMessage);
            chatMessage.setSendTime(System.currentTimeMillis());
            chatMessage.setContactId(groupId);
            chatMessage.setContactType(CONTACT_TYPE_GROUPS);
            chatMessage.setStatus(MessageStatusEnum.SEND_ED.getStatus());
            chatMessageMapper.insert(chatMessage);

            //将新建群组联系人加入redis中
            redisService.addUserContact(redisService.generateRedisKey(applyUserId , CONTACT_TYPE_GROUPS) ,groupInfo.getGroupId());
            //将联系人通道添加到群组通道
            channelContextUtils.addUser2Group(applyUserId , groupInfo.getGroupId());
            //发送群消息
            MessageSendDTO messageSendDTO = CopyTools.copy(chatMessage);
            messageSendDTO.setContactId(groupId);
            //获取群 人数
            Integer groupMemberCount = userContactMapper.getGroupCountByContactIdAndStatus(groupId, FRIEND_YES.getCode());
            messageSendDTO.setMemberCount(groupMemberCount);
            messageSendDTO.setContactName(groupInfo.getGroupName());
            //发消息
            messageHandler.sendMessage(messageSendDTO);
        }

        return ResultVo.success("处理好友申请成功");
    }




}
