package com.easychat.webSocket;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easychat.entity.*;
import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DO.ChatSessionUser;
import com.easychat.entity.DO.UserContactApply;
import com.easychat.entity.DO.UserInfo;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.ContactApplyStatusEnum;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.mapper.ChatMessageMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import com.easychat.mapper.UserContactApplyMapper;
import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.RedisService;
import com.easychat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Component
@Slf4j
public class ChannelContextUtils {
    @Autowired
    private RedisService redisService;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private ChatSessionUserMapper chatSessionUserMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private UserContactApplyMapper userContactApplyMapper;

    private static final ConcurrentHashMap<Integer,Channel> User_Context_Map = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer,ChannelGroup> Group_Context_Map = new ConcurrentHashMap<>();

    /***
     * channel与userId建立关联
     * @param userId
     * @param channel
     */
    public void addContext(Integer userId, Channel channel){
        String channelId = channel.id().toString();
        AttributeKey attributeKey = null;
        if(!AttributeKey.exists(channelId)){
            attributeKey = AttributeKey.newInstance(channelId);
        }else {
            attributeKey = AttributeKey.valueOf(channelId);
        }
        channel.attr(attributeKey).set(userId);

        // 定义不同的键名来存储好友和群组 ID
        String friendKey = redisService.generateRedisKey(userId,CONTACT_TYPE_FRIEND);
        String groupKey = redisService.generateRedisKey(userId,CONTACT_TYPE_GROUPS);
        List<Integer> friendIdList = redisService.getUserContactList(friendKey);
        List<Integer> groupIdList = redisService.getUserContactList(groupKey);

        for(Integer groupId : groupIdList){
            add2Group(groupId,channel);
        }

        //更新用户最后连接时间
        userInfoMapper.updateLastLoginTimeById(userId, LocalDateTime.now());

        //给用户发消息
        UserInfo user = userInfoMapper.getUserById(userId);
        if(user == null){
            return;
        }
        Long sourceLastOffTime = user.getLastOffTime();
        Long lastOffTime = sourceLastOffTime;
        if(sourceLastOffTime != null && System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000 > sourceLastOffTime){
            lastOffTime = System.currentTimeMillis();
        }
        /***
         * 1、查询会话信息
         */
        List<ChatSessionUser> chatSessionUserList = chatSessionUserMapper.getSessionListById(userId);

        WsInitDate wsInitDate = new WsInitDate();
        wsInitDate.setChatSessionUserList(chatSessionUserList);

        /***
         * 2、查询聊天信息
         */
        //查询所有聊天人
        List<Integer> allSessionList = groupIdList;
        allSessionList.add(userId);
//        ChatMessage chatMessage = new ChatMessage();
//        chatMessage.setContactIdList(allSessionList);
//        chatMessage.setLastReceiveTime(lastOffTime);
//        List<ChatMessage> chatMessageList = chatMessageMapper.selectList(chatMessage);
        List<ChatMessage> chatMessageList = chatMessageMapper.getChatMessages(allSessionList,lastOffTime);
        wsInitDate.setChatMessagesList(chatMessageList);

        /***
         * 3、查询好友申请
         */
        LambdaQueryWrapper<UserContactApply> apply = new LambdaQueryWrapper<>();
        apply.eq(UserContactApply::getReceiveUserId, userId)
                .ge(UserContactApply::getLastApplyTimestamp, lastOffTime)
                .eq(UserContactApply::getStatus, ContactApplyStatusEnum.WAITING.getStatus());

        Integer applyCount = Math.toIntExact(userContactApplyMapper.selectCount(apply));
        log.info("好友申请数：{}",applyCount);
        wsInitDate.setApplyCount(applyCount);


        //发送消息  WebSocket 初始化数据:
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setMessageType(MessageTypeEnum.INIT.getType());
        messageSendDTO.setContactId(userId);
        messageSendDTO.setExtendData(wsInitDate);
        log.info("WebSocket 初始化数据: {}", wsInitDate);
        sendMsg(messageSendDTO,userId);

    }

    private void add2Group(Integer groupId , Channel channel){
        ChannelGroup group = Group_Context_Map.get(groupId);
        if(group == null){
            group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            Group_Context_Map.put(groupId,group);
        }
        if(channel == null){
            return;
        }
        group.add(channel);
    }
    public void removeContext(Channel channel){
        Attribute<Integer> attribute = channel.attr(AttributeKey.valueOf(channel.id().toString()));
        Integer userId = attribute.get();
        if(userId != null){
            User_Context_Map.remove(userId);
        }
        //清除redis中用户登录的心跳
        redisService.removeUserHeartBeat(userId);
        //更新用户最后离线时间
        userInfoMapper.updateLastOffTimeById(userId,LocalDateTime.now());
    }

    //发送消息(单聊，群聊)
    public void sendMessage(MessageSendDTO sendDto){
        Integer contactType = chatMessageMapper.getContactTypeByContactId(sendDto.getContactId(),sendDto.getSessionId());
        if(contactType == null){
            log.info("ChannelContextUtils 未查到好友申请");
            return;
        }
        switch(contactType){
            case CONTACT_TYPE_FRIEND:
                send2User(sendDto);
                break;
            case CONTACT_TYPE_GROUPS:
                send2Group(sendDto);
                break;

        }
    }
    //发送给用户
    private void send2User(MessageSendDTO messageSendDTO){
        Integer contactId = messageSendDTO.getContactId();
        if(contactId == null){
            return;
        }
        sendMsg(messageSendDTO,contactId);

    }
    //发送给群聊
    private void send2Group(MessageSendDTO messageSendDTO){
        Integer contactId = messageSendDTO.getContactId();
        if(contactId == null){
            return;
        }
        ChannelGroup channelGroup = Group_Context_Map.get(contactId);
        if(channelGroup == null){
            return;
        }
        channelGroup.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(messageSendDTO)));
    }

    //发送消息
    public void sendMsg(MessageSendDTO messageSendDTO, Integer receiveId){
        if(receiveId == null){
            return;
        }
        Channel sendChannel = User_Context_Map.get(receiveId);
        if(sendChannel == null){
            return;
        }
        if(MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(messageSendDTO.getMessageType())){
            UserInfo userInfo = (UserInfo) messageSendDTO.getExtendData();
            messageSendDTO.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            messageSendDTO.setContactId(userInfo.getUserId());
            messageSendDTO.setContactName(userInfo.getNickName());
            messageSendDTO.setExtendData(null);



        } else {
            messageSendDTO.setContactId(messageSendDTO.getSendUserId());
            messageSendDTO.setContactName(messageSendDTO.getContactName());
        }

        sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(messageSendDTO)));
    }

}
