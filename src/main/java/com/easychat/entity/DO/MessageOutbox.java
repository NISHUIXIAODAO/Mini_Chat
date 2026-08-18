package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
@TableName("message_outbox")
public class MessageOutbox {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String eventId;
    private Long aggregateId;
    private String eventType;
    private String payload;
    private String status;
    private Integer retryCount;
    private Long nextRetryAt;
    private Long leaseUntil;
    private Long publishedAt;
    private String lastError;
    private String traceId;
    private Long createTime;
    private Long updateTime;
}
