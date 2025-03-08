package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 聊天消息表
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Getter
@Setter
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息自增ID
     */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 发送人ID
     */
    private Integer sendUserId;

    /**
     * 消息内容
     */
    private String messageContent;

    /**
     * 发送人昵称
     */
    private String sendUserNickName;

    /**
     * 发送时间
     */
    private Long sendTime;

    /**
     * 接收联系人ID
     */
    private Integer contactId;

    /**
     * 联系人类型：0-单聊，1-群聊
     */
    private Integer contactType;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private Boolean fileType;

    /**
     * 状态：0-正在发送，1-已发送
     */
    private Integer status;


    private List<Integer> contactIdList;

    private Long lastReceiveTime;
}
