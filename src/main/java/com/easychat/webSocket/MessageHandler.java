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
 * 多台服务器通过redis实现广播和订阅
 */
@Component("messageHandler")
public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final String MESSAGE_TOPIC = "message.topic";
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ChannelContextUtils channelContextUtils;

    /***
     * 消息订阅：
     * 监听器，当MESSAGE_TOPIC频道中收到新消息时，自动执行addListener方法
     * 通过channelContextUtils.sendMessage方法发送消息到目标客户端
     */
    @PostConstruct
    public void lisMessage(){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.addListener(MessageSendDTO.class,(MessageSend, sendDto) -> {
           logger.info("收到广播消息：{}", JsonUtils.convertObjToJson(sendDto));
           channelContextUtils.sendMessage(sendDto);
        });
    }

    /***\
     * 消息广播：
     * 调用此方法是将消息DTO发送到MESSAGE_TOPIC频道
     * @param sendDto
     */
    public void sendMessage(MessageSendDTO sendDto){
        RTopic rTopic = redissonClient.getTopic(MESSAGE_TOPIC);
        rTopic.publish(sendDto);
    }
}
