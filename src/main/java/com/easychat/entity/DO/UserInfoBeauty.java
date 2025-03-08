package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author scj
 * @since 2025-02-26
 */
@Getter
@Setter
@TableName("user_info_beauty")
public class UserInfoBeauty implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增ID
     */
    private Integer id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 0：未使用，1：已使用
     */
    private Boolean status;
}
