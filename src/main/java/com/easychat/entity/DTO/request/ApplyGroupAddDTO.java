package com.easychat.entity.DTO.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class ApplyGroupAddDTO {
    private Integer contactId;
    private String applyInfo;
//    private Integer contactType;

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public String getApplyInfo() {
        return applyInfo;
    }

    public void setApplyInfo(String applyInfo) {
        this.applyInfo = applyInfo;
    }

//    public Integer getContactType() {
//        return contactType;
//    }
//
//    public void setContactType(Integer contactType) {
//        this.contactType = contactType;
//    }
}
