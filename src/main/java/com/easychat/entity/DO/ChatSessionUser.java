package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * <p>
 * 会话用户表
 * </p>
 *
 * @author my
 * @since 2025-03-02
 */
@Getter
@Setter
@TableName("chat_session_user")
@Accessors(chain = true)
public class ChatSessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 联系人ID
     */
    private Integer contactId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 联系人名称
     */
    private String contactName;

    /***
     * 群成员数
     */
    private Integer memberCount;

    /***
     * 最后一条消息
     */
    private String lastMessage;

    /***
     * 最后一条消息时间
     */
    private Long LastReceiveTime;


}
