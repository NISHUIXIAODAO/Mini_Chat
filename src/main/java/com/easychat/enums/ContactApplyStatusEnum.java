package com.easychat.enums;

public enum ContactApplyStatusEnum {
    WAITING (0,"待处理"),
    AGREE (1,"已同意"),
    REFUSE (2,"已拒绝"),
    BLACK (3,"已拉黑");


    private final Integer status;

    private final String desc;

    ContactApplyStatusEnum(Integer status , String desc){
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
