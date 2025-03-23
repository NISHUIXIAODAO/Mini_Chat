package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.easychat.webSocket.ChannelContextUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

//@Component
//public class KafkaMessageConsumer {
//
//    private final CountDownLatch latch = new CountDownLatch(1);
//    private String lastMessage;
//
//    @KafkaListener(topics = "test")
//    public void receive(ConsumerRecord<String, String> record) {
//        lastMessage = record.value();
//        latch.countDown();
//    }
//
//    // 辅助方法，用于测试中等待消息到达
//    public CountDownLatch getLatch() {
//        return latch;
//    }
//
//    // 辅助方法，返回最后接收的消息
//    public String getLastMessage() {
//        return lastMessage;
//    }
//}
@Component
public class KafkaMessageConsumer {
    private final ChannelContextUtils channelContextUtils;
    public KafkaMessageConsumer(ChannelContextUtils channelContextUtils) {
        this.channelContextUtils = channelContextUtils;
    }
    @KafkaListener(topics = "message-topic", groupId = "group1")
    public void listen(MessageSendDTO message) {
        // 接收到 Kafka 的消息后，转发给目标客户端
        channelContextUtils.sendMessage(message);
    }
}


