package com.easychat.entity.DTO.response;

import io.swagger.models.auth.In;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserApplyListResponseDTO {
    private Integer applyUserId;

    private String applyInfo;

    private Integer status;

    private Long lastApplyTime;
}
