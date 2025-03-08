package com.easychat.enums;

public enum FriendStatusEnum {

        FRIEND_NO(0,"非好友"),
        FRIEND_YES(1,"好友"),
        FRIEND_DELETE(2,"已删除"),
        FRIEND_BLACK(3,"已拉黑");

        private final Integer code;
        private final String desc;


        FriendStatusEnum(Integer code , String desc){
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
