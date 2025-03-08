package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author my
 * @since 2025-02-27
 */
@Getter
@Setter
@TableName("user_contact")
public class UserContact implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 联系人ID或者群组ID
     */
    private Integer contactId;

    /**
     * 联系人类型 0：好友，1：群组
     */
    private Integer contactType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 状态：0：非好友 1：好友 2：已删除 3：拉黑
     */
    private Integer status;

    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdateTime;
}
