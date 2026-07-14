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
import com.easychat.mapper.*;
import com.easychat.service.IRedisService;
import com.easychat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_FRIEND;
import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Component
@Slf4j
public class ChannelContextUtils {
    public static final AttributeKey<Integer> USER_ID_KEY = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> CONNECTION_ID_KEY = AttributeKey.valueOf("connectionId");
    public static final AttributeKey<String> LOCATION_KEY = AttributeKey.valueOf("userLocation");

    private final IRedisService redisService;
    private final UserInfoMapper userInfoMapper;
    private final ChatSessionUserMapper chatSessionUserMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserContactApplyMapper userContactApplyMapper;
    private final UserContactMapper userContactMapper;

    public ChannelContextUtils(IRedisService redisService,
                               UserInfoMapper userInfoMapper,
                               ChatSessionUserMapper chatSessionUserMapper,
                               ChatMessageMapper chatMessageMapper,
                               UserContactApplyMapper userContactApplyMapper,
                               UserContactMapper userContactMapper) {
        this.redisService = redisService;
        this.userInfoMapper = userInfoMapper;
        this.chatSessionUserMapper = chatSessionUserMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userContactApplyMapper = userContactApplyMapper;
        this.userContactMapper = userContactMapper;
    }

    private static final ConcurrentHashMap<Integer,Channel> User_Context_Map = new ConcurrentHashMap<>();

    /***
     * channel 与 userId 建立关联    用户与自己的管道绑定
     * @param userId
     * @param channel
     */
    public void addContext(Integer userId, Channel channel){
        channel.attr(USER_ID_KEY).set(userId);
        channel.attr(CONNECTION_ID_KEY).set(channel.id().asShortText());

        User_Context_Map.put(userId, channel);

        List<Integer> friendIdList = redisService.getUserContactList(redisService.generateRedisKey(userId,CONTACT_TYPE_FRIEND));
        List<Integer> groupIdList = redisService.getUserContactList(redisService.generateRedisKey(userId,CONTACT_TYPE_GROUPS));

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

    /***
     * 关闭连接
     * @param channel
     */
    public void removeContext(Channel channel){
        Attribute<Integer> attribute = channel.attr(USER_ID_KEY);
        Integer userId = attribute.get();
        if (userId == null) {
            return;
        }
        String connectionId = channel.attr(CONNECTION_ID_KEY).get();
        User_Context_Map.remove(userId, channel);
        redisService.removeOnlineConnection(userId, connectionId);
        //更新用户最后离线时间
        userInfoMapper.updateLastOffTimeById(userId,LocalDateTime.now());
    }

    public void refreshContext(Channel channel) {
        Integer userId = channel.attr(USER_ID_KEY).get();
        String connectionId = channel.attr(CONNECTION_ID_KEY).get();
        redisService.refreshOnlineConnection(userId, connectionId);
    }

    public void saveConnectionLocation(Channel channel, String location) {
        Integer userId = channel.attr(USER_ID_KEY).get();
        String connectionId = channel.attr(CONNECTION_ID_KEY).get();
        channel.attr(LOCATION_KEY).set(location);
        redisService.saveOnlineConnection(userId, connectionId, location);
    }

    public boolean forceOffline(Integer userId, String reason) {
        Channel channel = User_Context_Map.get(userId);
        if (channel == null) {
            return false;
        }
        MessageSendDTO<String> messageSendDTO = new MessageSendDTO<>();
        messageSendDTO.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
        messageSendDTO.setContactId(userId);
        messageSendDTO.setMessageContent(reason);
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(messageSendDTO)));
        channel.close();
        return true;
    }

    /***
     * 通过 User_Context_Map 获取目标用户的管道 ，并将消息发送到对方管道
     * @param messageSendDTO
     * @param receiveId
     */
    //发送消息
    public void sendMsg(MessageSendDTO messageSendDTO, Integer receiveId){
        if(receiveId == null){
            return;
        }
        // 广播到各个服务端后，只有 服务器的User_Context_Map 存在当前接收用户的id，才会对消息进行一个处理
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
        } else if (messageSendDTO.getSendUserId() != null
                && !Integer.valueOf(CONTACT_TYPE_GROUPS).equals(messageSendDTO.getContactType())) {
            messageSendDTO.setContactId(messageSendDTO.getSendUserId());
            messageSendDTO.setContactName(messageSendDTO.getContactName());
        }
        /**
         * 将待发送的数据写入Channel并刷新缓冲区，确保数据被立即发送
         */
        sendChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(messageSendDTO)));
    }

}
