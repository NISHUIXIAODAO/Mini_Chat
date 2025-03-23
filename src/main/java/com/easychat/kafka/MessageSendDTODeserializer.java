package com.easychat.kafka;

import com.easychat.entity.DTO.request.MessageSendDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class MessageSendDTODeserializer implements Deserializer<MessageSendDTO> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // 配置方法，可根据需要实现
    }

    @Override
    public MessageSendDTO deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, MessageSendDTO.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing byte[] to MessageSendDTO", e);
        }
    }

    @Override
    public void close() {
        // 关闭方法，可根据需要实现
    }
}
