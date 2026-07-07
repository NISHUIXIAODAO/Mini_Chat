package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class GetMessageHistoryDTO {
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 联系人 ID
     */
    private Integer contactId;
    
    /**
     * 分页大小
     */
    private Integer pageSize = 20;
    
    /**
     * 最后一条消息的时间戳，用于分页查询
     * 首次查询可不传，默认查询最新的消息
     */
    private Long lastTimestamp;

    /**
     * 已收到的最大消息 ID，用于重连后拉取增量消息
     */
    private Long lastMessageId;

    /**
     * true 时查询 message_id 大于 lastMessageId 的新消息
     */
    private Boolean forward = false;
}
