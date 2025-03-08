package com.easychat.enums;


//没用！！！！！！！！！
public enum UserJoinTypeEnum {
    NO_NEED_AGREE(0,"直接添加"),
    NEED_AGREE(1,"需要同意");


    private final Integer code;
    private final String desc;

    UserJoinTypeEnum(Integer code,String desc){
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
