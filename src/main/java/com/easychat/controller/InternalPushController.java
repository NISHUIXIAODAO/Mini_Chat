package com.easychat.controller;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.entity.ResultVo;
import com.easychat.webSocket.ChannelContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部通信接口，用于集群间精准推送消息
 * 不应暴露给公网访问
 */
@RestController
@RequestMapping("/internal")
public class InternalPushController {

    @Autowired
    private ChannelContextUtils channelContextUtils;

    @PostMapping("/push")
    public ResultVo<String> pushMessage(@RequestBody MessageSendDTO message) {
        // 直接通过本地 WebSocket 推送
        channelContextUtils.sendMessage(message);
        return ResultVo.success("Pushed to local user");
    }
}
