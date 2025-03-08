package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class DisposeApplyDTO {
    private Integer applyUserId; // 申请者ID
    private Integer status;      // 该申请对应的处理状态

    public Integer getApplyUserId() {
        return applyUserId;
    }

    public void setApplyUserId(Integer applyUserId) {
        this.applyUserId = applyUserId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
