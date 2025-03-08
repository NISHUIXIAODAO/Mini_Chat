package com.easychat.webSocket;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.utils.JsonUtils;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/***
 * 集群化
 * 多台服务器通过redis实现广播
 */
@Component("messageHandler")
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ChannelContextUtils channelContextUtils;

    @PostConstruct
    public void lisMessage(){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.addListener(MessageSendDTO.class,(MessageSend, sendDto) -> {
           logger.info("收到广播消息：{}", JsonUtils.convertObjToJson(sendDto));
           channelContextUtils.sendMessage(sendDto);
        });
    }

    public void sendMessage(MessageSendDTO sendDto){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDto);
    }
}
