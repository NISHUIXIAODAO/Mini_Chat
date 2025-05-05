package com.easychat.entity.DTO.request;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
public class LoginDTO {

    private String email;
    private String password;

}
