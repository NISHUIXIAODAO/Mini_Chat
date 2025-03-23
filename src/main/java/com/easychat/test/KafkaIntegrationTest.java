package com.easychat.test;
//
//import com.easychat.kafka.KafkaMessageConsumer;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.test.annotation.DirtiesContext;
//
//import java.util.concurrent.TimeUnit;
//
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@SpringBootTest
//@DirtiesContext
class KafkaIntegrationTest {}
//
//    @Autowired
//    private KafkaTemplate<String, String> kafkaTemplate;
//
//    @Autowired
//    private KafkaMessageConsumer consumer; // 你自己实现的消费者
//
//    @Test
//    void testKafkaMessage() throws Exception {
//        String testMessage = "Hello, Kafka!";
//        String topic = "test"; // 替换为你实际的 Topic
//
//        // 发送测试消息
//        kafkaTemplate.send(topic, testMessage);
//
//        // 等待消费者接收消息，比如使用 CountDownLatch 等待一定时间
//        boolean received = consumer.getLatch().await(5, TimeUnit.SECONDS);
//        System.out.println("消息：{}" + consumer.getLastMessage());
//        assertTrue(received, "消费者未在规定时间内接收到消息");
//        assertEquals(testMessage, consumer.getLastMessage());
//
//    }
//}
