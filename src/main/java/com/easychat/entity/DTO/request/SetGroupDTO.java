package com.easychat.entity.DTO.request;

import lombok.Data;

@Data
public class SetGroupDTO {
    private String groupName;
    private String groupNotice;
    private Boolean joinType;
}
