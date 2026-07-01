package com.easychat.service.impl;

import com.easychat.mapper.UserInfoMapper;
import com.easychat.service.IUserEmailBloomService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Service
public class UserEmailBloomServiceImpl implements IUserEmailBloomService {

    private final RedissonClient redissonClient;
    private final UserInfoMapper userInfoMapper;

    @Value("${bloom.user-email.name:bf:user:email}")
    private String bloomName;

    @Value("${bloom.user-email.expected-insertions:200000}")
    private long expectedInsertions;

    @Value("${bloom.user-email.false-positive-probability:0.01}")
    private double falsePositiveProbability;

    @Value("${bloom.user-email.preload-enabled:true}")
    private boolean preloadEnabled;

    private volatile boolean enabled = true;
    private RBloomFilter<String> bloomFilter;

    public UserEmailBloomServiceImpl(RedissonClient redissonClient, UserInfoMapper userInfoMapper) {
        this.redissonClient = redissonClient;
        this.userInfoMapper = userInfoMapper;
    }

    @PostConstruct
    public void init() {
        if (redissonClient == null) {
            enabled = false;
            log.error("RedissonClient is null, bloom filter disabled");
            return;
        }
        bloomFilter = redissonClient.getBloomFilter(bloomName);
        bloomFilter.tryInit(expectedInsertions, falsePositiveProbability);
        if (!preloadEnabled) {
            return;
        }
        try {
            List<String> emails = userInfoMapper.getAllEmails();
            if (emails == null || emails.isEmpty()) {
                return;
            }
            for (String email : emails) {
                String normalized = normalize(email);
                if (normalized != null) {
                    bloomFilter.add(normalized);
                }
            }
            log.info("BloomFilter preload completed, size={}", emails.size());
        } catch (Exception e) {
            log.error("BloomFilter preload failed", e);
        }
    }

    @Override
    public boolean mightContain(String email) {
        if (!enabled) {
            return true;
        }
        String normalized = normalize(email);
        if (normalized == null) {
            return true;
        }
        return bloomFilter.contains(normalized);
    }

    @Override
    public void add(String email) {
        if (!enabled) {
            return;
        }
        String normalized = normalize(email);
        if (normalized == null) {
            return;
        }
        bloomFilter.add(normalized);
    }

    private String normalize(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }
}

