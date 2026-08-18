package com.easychat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "easychat.kafka")
public class KafkaPushProperties {
    private String pushTopic = "easychat.push.event";
    private String consumerGroupPrefix = "easychat-push";
    private int consumerRetryAttempts = 3;
    private long consumerRetryIntervalMillis = 1000L;

    public String getPushTopic() { return pushTopic; }
    public void setPushTopic(String pushTopic) { this.pushTopic = pushTopic; }
    public String getConsumerGroupPrefix() { return consumerGroupPrefix; }
    public void setConsumerGroupPrefix(String consumerGroupPrefix) { this.consumerGroupPrefix = consumerGroupPrefix; }
    public int getConsumerRetryAttempts() { return consumerRetryAttempts; }
    public void setConsumerRetryAttempts(int consumerRetryAttempts) { this.consumerRetryAttempts = consumerRetryAttempts; }
    public long getConsumerRetryIntervalMillis() { return consumerRetryIntervalMillis; }
    public void setConsumerRetryIntervalMillis(long consumerRetryIntervalMillis) { this.consumerRetryIntervalMillis = consumerRetryIntervalMillis; }
}
