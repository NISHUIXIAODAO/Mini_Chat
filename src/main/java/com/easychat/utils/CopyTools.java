package com.easychat.utils;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.DTO.response.MessageHistoryResponseDTO;
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

    public static void copyProperties(ChatMessage message, MessageHistoryResponseDTO dto) {
        dto.setSendUserId(message.getSendUserId())
                .setContactId(message.getContactId())
                .setContactType(message.getContactType())
                .setMessageId(message.getMessageId())
                .setMessageContent(message.getMessageContent())
                .setMessageType(message.getMessageType())
                .setSessionId(message.getSessionId())
                .setSendUserNickName(message.getSendUserNickName())
                .setFileType(message.getFileType())
                .setFileName(message.getFileName())
                .setFileSize(message.getFileSize())
                .setSendTime(message.getSendTime())
                .setStatus(message.getStatus());
    }
}
