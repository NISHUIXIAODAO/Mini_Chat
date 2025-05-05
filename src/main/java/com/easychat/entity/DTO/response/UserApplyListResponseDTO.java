package com.easychat.entity.DTO.response;

import io.swagger.models.auth.In;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UserApplyListResponseDTO {
    private Integer applyUserId;

    private String applyInfo;

    private Integer status;

    private Long lastApplyTime;
}
