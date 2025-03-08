package com.easychat.entity.DTO.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendDTO<T> implements Serializable {
    private static final long serialVersionUID = -1045752033171142417L;

    // 消息ID
    private Long messageId;

    // 会话ID
    private String sessionId;

    // 发送人
    private Integer sendUserId;

    // 发送人昵称
    private String sendUserNickName;

    // 联系人ID
    private Integer contactId;

    // 联系人名称
    private String contactName;

    // 消息内容
    private String messageContent;

    // 最后的消息
    private String lastMessage;

    // 消息类型
    private Integer messageType;

    // 发送时间
    private Long sendTime;

    // 联系人类型
    private Integer contactType;

    // 扩展信息
    private T extendData;

    // 消息状态 0：发送中 1：已发送（对于文件是异步上传用状态处理）
    private Integer status;

    // 文件信息
    private Long fileSize;
    private String fileName;
    private Integer fileType;

    // 群员
    private Integer memberCount;
}

