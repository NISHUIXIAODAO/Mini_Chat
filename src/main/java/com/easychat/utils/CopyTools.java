package com.easychat.utils;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.MessageSendDTO;
import org.springframework.stereotype.Component;

/***
 * 此工具类目的是，将 chatMessage复制给 messageSendDTO
 * chatMessage负责存入数据库，messageSendDTO负责发送消息
 */
@Component
public class CopyTools {
    public static MessageSendDTO copy(ChatMessage chatMessage){
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setSessionId(chatMessage.getSessionId())
                .setMessageType(chatMessage.getMessageType())
                .setMessageContent(chatMessage.getMessageContent())
                .setSendUserId(chatMessage.getSendUserId())
                .setSendUserNickName(chatMessage.getSendUserNickName())
                .setContactId(chatMessage.getContactId())
                .setSendTime(chatMessage.getSendTime())
                .setContactType(chatMessage.getContactType());
        return messageSendDTO;
    }
}
