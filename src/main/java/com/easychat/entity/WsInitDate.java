package com.easychat.entity;

import com.easychat.entity.DO.ChatMessage;
import com.easychat.entity.DO.ChatSessionUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class WsInitDate {
    private List<ChatSessionUser> chatSessionUserList;

    private List<ChatMessage> chatMessagesList;

    private Integer applyCount;


    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString(); // JSON 解析失败时，返回默认 toString()
        }
    }

    public List<ChatSessionUser> getChatSessionUserList() {
        return chatSessionUserList;
    }

    public void setChatSessionUserList(List<ChatSessionUser> chatSessionUserList) {
        this.chatSessionUserList = chatSessionUserList;
    }

    public List<ChatMessage> getChatMessagesList() {
        return chatMessagesList;
    }

    public void setChatMessagesList(List<ChatMessage> chatMessagesList) {
        this.chatMessagesList = chatMessagesList;
    }

    public Integer getApplyCount() {
        return applyCount;
    }

    public void setApplyCount(Integer applyCount) {
        this.applyCount = applyCount;
    }
}
