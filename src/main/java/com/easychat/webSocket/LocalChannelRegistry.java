package com.easychat.webSocket;

import com.easychat.entity.DO.UserInfo;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

import static com.easychat.utils.ConstantUtils.CONTACT_TYPE_GROUPS;

@Component
public class LocalChannelRegistry {
    public static final AttributeKey<Integer> USER_ID_KEY = AttributeKey.valueOf("userId");
    public static final AttributeKey<String> CONNECTION_ID_KEY = AttributeKey.valueOf("connectionId");

    private final ConcurrentHashMap<Integer, Channel> channels = new ConcurrentHashMap<>();

    public void register(Integer userId, Channel channel) {
        bindAuthenticatedUser(userId, channel);
        channels.put(userId, channel);
    }

    public void bindAuthenticatedUser(Integer userId, Channel channel) {
        channel.attr(USER_ID_KEY).set(userId);
        channel.attr(CONNECTION_ID_KEY).set(channel.id().asShortText());
    }

    public void remove(Channel channel) {
        Integer userId = channel.attr(USER_ID_KEY).get();
        if (userId != null) {
            channels.remove(userId, channel);
        }
    }

    public String getConnectionId(Channel channel) {
        return channel.attr(CONNECTION_ID_KEY).get();
    }

    public boolean send(MessageSendDTO<?> message, Integer userId) {
        if (userId == null) {
            return false;
        }
        Channel channel = channels.get(userId);
        if (channel == null || !channel.isActive()) {
            return false;
        }
        prepareForRecipient(message);
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(message)));
        return true;
    }

    public boolean forceOffline(Integer userId, String reason) {
        Channel channel = channels.get(userId);
        if (channel == null || !channel.isActive()) {
            return false;
        }
        MessageSendDTO<String> message = new MessageSendDTO<>();
        message.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
        message.setContactId(userId);
        message.setMessageContent(reason);
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObjToJson(message)));
        channel.close();
        return true;
    }

    private void prepareForRecipient(MessageSendDTO<?> message) {
        if (MessageTypeEnum.ADD_FRIEND_SELF.getType().equals(message.getMessageType())) {
            UserInfo userInfo = (UserInfo) message.getExtendData();
            message.setMessageType(MessageTypeEnum.ADD_FRIEND.getType());
            message.setContactId(userInfo.getUserId());
            message.setContactName(userInfo.getNickName());
            message.setExtendData(null);
        } else if (message.getSendUserId() != null
                && !Integer.valueOf(CONTACT_TYPE_GROUPS).equals(message.getContactType())) {
            message.setContactId(message.getSendUserId());
            message.setContactName(message.getContactName());
        }
    }
}
