package com.easychat.controller;

import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.GetMessageHistoryDTO;
import com.easychat.entity.DTO.response.MessageHistoryResponseDTO;
import com.easychat.entity.ResultVo;
import com.easychat.service.IChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Autowired
    private IChatMessageService chatMessageService;

    @RequestMapping("/sendMessage")
    public ResultVo<Object> sendMessage(@RequestBody ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response) {
        return ResultVo.success(chatMessageService.saveMessage(chatSendMessageDTO, request, response));
    }
    
    /**
     * 获取聊天历史记录
     * @param contactId 联系人ID
     * @param request HTTP请求
     * @return 聊天历史记录列表
     */
    @RequestMapping("/getMessageHistory")
    public ResultVo<List<MessageHistoryResponseDTO>> getMessageHistory(Integer contactId, HttpServletRequest request) {
        GetMessageHistoryDTO getMessageHistoryDTO = new GetMessageHistoryDTO();
        getMessageHistoryDTO.setContactId(contactId);
        List<MessageHistoryResponseDTO> messageList = chatMessageService.getMessageHistory(getMessageHistoryDTO, request);
        return ResultVo.success(messageList);
    }

}
