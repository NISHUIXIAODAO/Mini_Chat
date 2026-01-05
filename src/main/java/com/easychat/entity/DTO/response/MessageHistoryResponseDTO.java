package com.easychat.entity.DTO.response;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Getter
@Setter
@Accessors(chain = true)
public class MessageHistoryResponseDTO {
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
    private Integer fileType;

    /**
     * 状态：0-正在发送，1-已发送
     */
    private Integer status;

}
