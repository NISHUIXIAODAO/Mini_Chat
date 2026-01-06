package com.easychat.service;

public interface AIService {
    /**
     * 调用 AI 模型进行对话
     *
     * @param content 用户发送的内容
     * @return AI 的回复
     */
    String chat(String content);
}
