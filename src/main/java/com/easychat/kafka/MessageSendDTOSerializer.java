package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/****
 * kafka序列化器
 */
public class MessageSendDTOSerializer implements Serializer<MessageSendDTO> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // 配置方法，可根据需要实现
    }

    @Override
    public byte[] serialize(String topic, MessageSendDTO data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error serializing MessageSendDTO", e);
        }
    }

    @Override
    public void close() {
        // 关闭方法，可根据需要实现
    }
}
