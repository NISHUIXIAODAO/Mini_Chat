package com.easychat.service.domain;

import com.easychat.mapper.ChatSessionMapper;
import com.easychat.mapper.ChatSessionUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SessionDomainService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionUserMapper chatSessionUserMapper;

    public SessionDomainService(ChatSessionMapper chatSessionMapper, ChatSessionUserMapper chatSessionUserMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatSessionUserMapper = chatSessionUserMapper;
    }

    public void saveOrUpdateSession(String sessionId, String lastMessage, long lastReceiveTime) {
        chatSessionMapper.upsertBySessionId(sessionId, lastMessage, lastReceiveTime);
    }

    public void saveOrUpdateSessionUser(Integer userId, Integer contactId, String sessionId, String contactName) {
        chatSessionUserMapper.upsertByUserIdAndContactId(userId, contactId, sessionId, contactName);
    }
}
