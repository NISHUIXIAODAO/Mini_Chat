package com.easychat.service.impl;

import com.easychat.entity.DO.*;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.entity.DTO.request.SetGroupDTO;
import com.easychat.enums.MessageStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.mapper.*;
import com.easychat.service.IGroupInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easychat.service.RedisService;
import com.easychat.utils.ConstantUtils;
import com.easychat.utils.CopyTools;
import com.easychat.webSocket.ChannelContextUtils;
import com.easychat.webSocket.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

import static com.easychat.enums.FriendStatusEnum.FRIEND_YES;
import static com.easychat.enums.MessageTypeEnum.GROUP_CREATE;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;
import static com.easychat.utils.SessionIdUtils.generateSessionId;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GroupInfoServiceImpl extends ServiceImpl<GroupInfoMapper, GroupInfo> implements IGroupInfoService {
    @Autowired
    private JWTServiceImpl jwtService;
    @Autowired
    private UserContactMapper userContactMapper;
    @Autowired
    private ChatSessionMapper chatSessionMapper;
    @Autowired
    private ChatSessionUserMapper chatSessionUserMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private ChannelContextUtils channelContextUtils;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private CopyTools copyTools;
    @Autowired
    private MessageHandler messageHandler;

    private final GroupInfoMapper groupInfoMapper;

    /***
     * 新建群聊 并创建会话 发送ws消息
     * @param setGroupDTO
     * @param request
     * @param response
     * @return
     */
    @Override
    public ResultVo setGroup(SetGroupDTO setGroupDTO,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        //通过解析token，获得群主id
        String token = request.getHeader("token");
        Integer userId = jwtService.getUserId(token);

        //添加群组信息
        GroupInfo group = GroupInfo.builder()
                .groupName(setGroupDTO.getGroupName())
                .groupOwnerId(userId)
                .createTime(LocalDateTime.now())
                .groupNotice(setGroupDTO.getGroupNotice())
                .joinType(setGroupDTO.getJoinType())
                .build();
        groupInfoMapper.insert(group);

        GroupInfo groupInfo = groupInfoMapper.getByNameAndOwnerId(userId, setGroupDTO.getGroupName());

        //将群组添加为群主的联系人
        UserContact userContact = new UserContact();
        userContact.setUserId(userId);
        userContact.setContactId(groupInfo.getGroupId());
        userContact.setContactType(CONTACT_TYPE_GROUPS);
        userContact.setStatus(FRIEND_YES.getCode());
        userContact.setCreateTime(LocalDateTime.now());
        userContact.setLastUpdateTime(LocalDateTime.now());
        userContactMapper.insert(userContact);


        //创建会话
        String sessionId = generateSessionId(userId,groupInfo.getGroupId());
        if(chatSessionMapper.boolSessionId(sessionId) != null){
            chatSessionMapper.updateBySessionId(sessionId,GROUP_CREATE.getInitMessage(),System.currentTimeMillis());
        }
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastMessage(GROUP_CREATE.getInitMessage());
        chatSession.setLastReceiveTime(System.currentTimeMillis());
        chatSessionMapper.insert(chatSession);

        //创建群主会话
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(groupInfo.getGroupOwnerId());
        chatSessionUser.setContactId(groupInfo.getGroupId());
        chatSessionUser.setContactName(group.getGroupName());
        chatSessionUser.setSessionId(sessionId);
        chatSessionUserMapper.insert(chatSessionUser);

        //创建消息
        ChatMessage chatMassage = new ChatMessage();
        chatMassage.setSessionId(sessionId);
        chatMassage.setMessageType(GROUP_CREATE.getType());
        chatMassage.setMessageContent(GROUP_CREATE.getInitMessage());
        chatMassage.setSendTime(System.currentTimeMillis());
        chatMassage.setContactId(groupInfo.getGroupId());
        chatMassage.setContactType(CONTACT_TYPE_GROUPS);
        chatMassage.setStatus(MessageStatusEnum.SEND_ED.getStatus());
        chatMessageMapper.insert(chatMassage);

        //将新建群组联系人加入redis中
        redisService.addUserContact(redisService.generateRedisKey(groupInfo.getGroupOwnerId() , CONTACT_TYPE_GROUPS) ,groupInfo.getGroupId());
        //将联系人通道添加到群组通道
        channelContextUtils.addUser2Group(groupInfo.getGroupOwnerId(), group.getGroupId());

        //发送ws消息
        chatSessionUser.setLastMessage(GROUP_CREATE.getInitMessage());
        chatSessionUser.setLastReceiveTime(System.currentTimeMillis());
        chatSessionUser.setMemberCount(1);

        MessageSendDTO messageSendDTO = copyTools.copy(chatMassage);
        messageSendDTO.setExtendData(chatSessionUser);
        messageSendDTO.setLastMessage(chatSessionUser.getLastMessage());
        messageHandler.sendMessage(messageSendDTO);


        return ResultVo.success("注册成功");
    }
}
