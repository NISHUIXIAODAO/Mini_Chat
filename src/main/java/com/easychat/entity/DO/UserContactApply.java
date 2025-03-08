package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 联系人申请
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
@Getter
@Setter
@TableName("user_contact_apply")
public class UserContactApply implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 自增 ID
     */
    @TableId(value = "apply_id", type = IdType.AUTO)
    private Integer applyId;

    /**
     * 申请人 id
     */
    private Integer applyUserId;

    /**
     * 接收人 ID
     */
    private Integer receiveUserId;

    /**
     * 联系人类型 0: 好友 1: 群组
     */
    private Integer contactType;

    /**
     * 联系人群组 ID
     */
    private Integer contactId;

    /**
     * 最后申请时间
     */
    private Long lastApplyTime;

    /**
     * 状态 0: 待处理 1: 已同意 2: 已拒绝 3: 已拉黑
     */
    private Integer status;

    /**
     * 申请信息
     */
    private String applyInfo;

    private Long lastApplyTimestamp;
}
