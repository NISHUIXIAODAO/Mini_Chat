package com.easychat.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig {
    @Value("${spring.redis.host:}")
    private String redisHost;
    @Value("${spring.redis.port:}")
    private Integer redisPort;
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean(name = "redissonClient" , destroyMethod = "shutdown")
    public RedissonClient redissonClient(){
        try{
            Config config = new Config();
            config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort);
            //todo 这里要加上setPassword()给Redisson也配置上密码，不然后面test请求会报错
            RedissonClient redissonClient = Redisson.create(config);
            return redissonClient;
        }catch (Exception e){
            logger.info("redisson配置错误",e);
        }
        return null;
    }


    @Bean("redisTemplate")
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板类");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //设置redis连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }
}
