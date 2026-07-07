package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@TableName("chat_message_user_status")
@Accessors(chain = true)
public class ChatMessageUserStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long messageId;
    private String sessionId;
    private Integer userId;
    private Integer status;
    private Long deliveredTime;
    private Long readTime;
}
