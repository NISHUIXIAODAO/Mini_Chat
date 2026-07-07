package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class DisposeApplyDTO {
    private Integer applyId;     // 申请记录ID
    private Integer applyUserId; // 申请者ID
    private Integer contactId;   // 好友ID或群ID
    private Integer contactType; // 0: 好友 1: 群组
    private Integer status;      // 该申请对应的处理状态

    public Integer getApplyId() {
        return applyId;
    }

    public void setApplyId(Integer applyId) {
        this.applyId = applyId;
    }

    public Integer getApplyUserId() {
        return applyUserId;
    }

    public void setApplyUserId(Integer applyUserId) {
        this.applyUserId = applyUserId;
    }

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public Integer getContactType() {
        return contactType;
    }

    public void setContactType(Integer contactType) {
        this.contactType = contactType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
