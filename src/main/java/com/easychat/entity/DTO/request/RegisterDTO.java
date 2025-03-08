package com.easychat.entity.DTO.request;
import lombok.Data;

@Data
public class RegisterDTO {
    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    private String nickName;
    /**
     * 0：女，1：男
     */
    private Boolean sex;

    /**
     * 密码
     */
    private String password;

    /***
     * 验证码
     */
    private String code;

    /***
     * 0可以直接被添加，1需要验证
     */
    private Integer joinType;

}
