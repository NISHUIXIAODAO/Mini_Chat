package com.easychat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "easychat.outbox")
public class OutboxProperties {
    private long scanIntervalMillis = 1000L;
    private int batchSize = 100;
    private long leaseSeconds = 30L;
    private int maxRetryCount = 10;
    private long initialBackoffMillis = 1000L;
    private long maxBackoffMillis = 300000L;

    public long getScanIntervalMillis() { return scanIntervalMillis; }
    public void setScanIntervalMillis(long scanIntervalMillis) { this.scanIntervalMillis = scanIntervalMillis; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public long getLeaseSeconds() { return leaseSeconds; }
    public void setLeaseSeconds(long leaseSeconds) { this.leaseSeconds = leaseSeconds; }
    public int getMaxRetryCount() { return maxRetryCount; }
    public void setMaxRetryCount(int maxRetryCount) { this.maxRetryCount = maxRetryCount; }
    public long getInitialBackoffMillis() { return initialBackoffMillis; }
    public void setInitialBackoffMillis(long initialBackoffMillis) { this.initialBackoffMillis = initialBackoffMillis; }
    public long getMaxBackoffMillis() { return maxBackoffMillis; }
    public void setMaxBackoffMillis(long maxBackoffMillis) { this.maxBackoffMillis = maxBackoffMillis; }
}
