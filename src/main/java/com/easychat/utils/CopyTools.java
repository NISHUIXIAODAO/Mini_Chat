package com.easychat.utils;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.MessageSendDTO;
import org.springframework.stereotype.Component;

@Component
public class CopyTools {
    public static MessageSendDTO copy(ChatMessage chatMessage){
        MessageSendDTO messageSendDTO = new MessageSendDTO();

        messageSendDTO.setSessionId(chatMessage.getSessionId());
        messageSendDTO.setMessageType(chatMessage.getMessageType());
        messageSendDTO.setMessageContent(chatMessage.getMessageContent());
        messageSendDTO.setSendUserId(chatMessage.getSendUserId());
        messageSendDTO.setSendUserNickName(chatMessage.getSendUserNickName());
        messageSendDTO.setContactId(chatMessage.getContactId());
        messageSendDTO.setSendTime(chatMessage.getSendTime());
        messageSendDTO.setContactType(chatMessage.getContactType());

        return messageSendDTO;
    }
}
