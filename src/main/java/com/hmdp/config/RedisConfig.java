package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @Author FanJian
 * @Date 2022/10/16 14:50
 */

@Configuration
public class RedisConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.200.130:6379");
        return Redisson.create(config);
    }
    /**
     * 设置 redisTemplate 的序列化设置
     * @param redisConnectionFactory
     * @return
     */
/*
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 1.创建 redisTemplate 模版
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        // 2.关联 redisConnectionFactory
        template.setConnectionFactory(redisConnectionFactory);
        // 3.创建 序列化类
        GenericToStringSerializer genericToStringSerializer = new GenericToStringSerializer(Object.class);
        // 6.序列化类，对象映射设置
        // 7.设置 value 的转化格式和 key 的转化格式
        template.setValueSerializer(genericToStringSerializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
*/
/*
    @Bean
    public RedisTemplate<Object,Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object,Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        //设置 value 的序列化方式为 JOSON
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        //设置 key 的序列化方式为 String
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
*/

}

