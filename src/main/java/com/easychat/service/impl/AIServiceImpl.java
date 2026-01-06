package com.easychat.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.easychat.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AIServiceImpl implements AIService {

    // 从 application.yml 或 application.properties 中读取配置
    // 这里为了方便演示，如果您的配置文件中没有这些配置，可以使用默认值或者硬编码
    // 建议您将这些配置添加到 application.properties 中
    @Value("${open-ai.chat-model.base-url:https://api.deepseek.com/chat/completions}")
    private String baseUrl;

    @Value("${open-ai.chat-model.api-key:sk-4a32035e274e44e6accd70ef3c5d1a14}")
    private String apiKey;

    @Value("${open-ai.chat-model.model-name:deepseek-chat}")
    private String modelName;

    @Override
    public String chat(String content) {
        try {
            // 构造请求体 (OpenAI Chat Completion 格式)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            
            // 构造消息列表
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", content);
            
            requestBody.put("messages", new Object[]{userMessage});
            
            // 发送请求
            String jsonBody = JSONUtil.toJsonStr(requestBody);
            log.info("AI Request: {}", jsonBody);

            // 注意：Hutool 的 HttpRequest 默认超时可能较短，大模型建议设置长一点
            // 阿里云百炼/OpenAI 兼容接口通常需要 Authorization: Bearer <token>
            HttpResponse response = HttpRequest.post(baseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(jsonBody)
                    .timeout(60000) // 60秒超时
                    .execute();

            String responseBody = response.body();
            log.info("AI Response: {}", responseBody);

            if (response.isOk()) {
                // 解析响应
                JSONObject jsonObject = JSONUtil.parseObj(responseBody);
                JSONArray choices = jsonObject.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    return message.getStr("content");
                }
            } else {
                log.error("AI API调用失败，状态码: {}, 响应: {}", response.getStatus(), responseBody);
                return "抱歉，我现在无法思考（API调用失败: " + response.getStatus() + "）";
            }
        } catch (Exception e) {
            log.error("AI服务异常", e);
            return "抱歉，我遇到了一点问题：" + e.getMessage();
        }
        return "抱歉，我不知道该说什么。";
    }
}
