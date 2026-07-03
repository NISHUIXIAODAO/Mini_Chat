package com.easychat.service.domain;

import com.easychat.entity.DO.ChatSession;
import com.easychat.entity.DO.ChatSessionUser;
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
        if (chatSessionMapper.boolSessionId(sessionId) != null) {
            chatSessionMapper.updateBySessionId(sessionId, lastMessage, lastReceiveTime);
            return;
        }
        ChatSession chatSession = new ChatSession();
        chatSession.setSessionId(sessionId);
        chatSession.setLastMessage(lastMessage);
        chatSession.setLastReceiveTime(lastReceiveTime);
        chatSessionMapper.insert(chatSession);
    }

    public void saveOrUpdateSessionUser(Integer userId, Integer contactId, String sessionId, String contactName) {
        if (chatSessionUserMapper.boolByUserIdAndContactId(userId, contactId) != null) {
            chatSessionUserMapper.updateByUserIdAndContactId(userId, contactId, contactName);
            return;
        }
        ChatSessionUser chatSessionUser = new ChatSessionUser();
        chatSessionUser.setUserId(userId);
        chatSessionUser.setContactId(contactId);
        chatSessionUser.setSessionId(sessionId);
        chatSessionUser.setContactName(contactName);
        chatSessionUserMapper.insert(chatSessionUser);
    }
}
