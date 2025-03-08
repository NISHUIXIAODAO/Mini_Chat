package com.easychat.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.*;

/**
 * <p>
 * 
 * </p>
 *
 * @author scj
 * @since 2025-02-27
 */
@Getter
@Setter
@Builder
@TableName("group_info")
@NoArgsConstructor
@AllArgsConstructor
public class GroupInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 群ID
     */
    private Integer groupId;

    /**
     * 群组名
     */
    private String groupName;

    /**
     * 群主ID
     */
    private Integer groupOwnerId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 群公告
     */
    private String groupNotice;

    /**
     * 0：直接加入 1：管理员同意
     */
    private Boolean joinType;

    /**
     * 1:正常 0：解散
     */
    private Boolean status;
}
