package com.easychat.controller;

import com.easychat.entity.DTO.request.ChatSendMessageDTO;
import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.enums.MessageTypeEnum;
import com.easychat.hander.GlobalExceptionHandler;
import com.easychat.service.IChatMessageService;
import com.easychat.service.IChatSessionUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/chat")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private IChatMessageService chatMessageService;

    @RequestMapping("/sendMessage")
    public ResultVo<Object> sendMessage(@RequestBody ChatSendMessageDTO chatSendMessageDTO, HttpServletRequest request, HttpServletResponse response){
        try{
            MessageSendDTO messageSendDTO = chatMessageService.saveMessage(chatSendMessageDTO,request,response);
            return ResultVo.success("发送消息成功" + messageSendDTO);
        } catch (Exception e){
            return ResultVo.failed("发送消息失败:" + e);
        }
    }

}
