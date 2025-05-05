package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 会话信息
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Getter
@Setter
@TableName("chat_session")
@Accessors(chain = true)
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最后接受的消息
     */
    private String lastMessage;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 最后接受消息时间（毫秒）
     */
    private Long lastReceiveTime;
}
