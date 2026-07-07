package com.easychat.entity.DTO.response;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserApplyListResponseDTO {
    private Integer applyId;

    private Integer applyUserId;

    private Integer contactId;

    private Integer contactType;

    private String applyInfo;

    private Integer status;

    private Long lastApplyTime;
}
