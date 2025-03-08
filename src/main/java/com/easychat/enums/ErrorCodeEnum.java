package com.easychat.enums;

public enum ErrorCodeEnum {
    //枚举出全部错误类型
    //自定义状态吗和报错信息
    PARAMS_ERROR(401, "有同名用户！"),
    ACCOUNT_PWD_NOT_EXIST(401, "用户名密码不能为空！"),
    SUCCESS(200, "操作成功"),

    FAILED(500, "用户名不存在或密码错误！"),

    UNAUTHORIZED(401, "请输入合法的用户名和密码"),

    FORBIDDEN(401, "操作失败！"),
    ;
    private int code;
    private String msg;

    ErrorCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }


    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }

}
