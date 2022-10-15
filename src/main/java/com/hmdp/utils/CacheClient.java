package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * @Author FanJian
 * @Date 2022/10/14 18:03
 */

@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;
    public CacheClient (StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long expireTime, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),expireTime,unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long expireTime, TimeUnit unit) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        RedisData redisData = new RedisData();
        // 生产环境下应该设置30分钟逻辑过期时间
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(expireTime)));
        redisData.setData(value);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    public <T,ID> T queryWithPassThrough(String keyPrefix, ID id, Class<T> type, Function<ID,T> dbCallBack,Long expireTime, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);

        // 如果redis中有数据
        // isNotBlank为true的情况
        // 1. 参数不为""
        // 2. 参数不为null
        if (StringUtils.isNotBlank(json)) {
            T data = JSONUtil.toBean(json, type);
            return data;
        }

        // 走到这里说明两种可能，1.redis没有数据(null) 2.redis中的数据为“”
        if (json != null) {
            return null;
        }

        T data = dbCallBack.apply(id);
        // 如果数据库中没有数据
        if (data == null) {
            // 防止缓存穿透，数据库中如果没有找到数据就在redis存储空值
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            return null;
        }
        // 如果数据库中有数据
        else {
            // 缓存到redis中，并给一个过期时间
            set(key,data,expireTime,unit);
            return data;
        }
    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <T,ID> T queryWithLogical(String keyPrefix,String lockKeyPrefix,ID id,Class<T> type,Function<ID,T> dbCallBack, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String jsonRedisData = stringRedisTemplate.opsForValue().get(key);

        // 1. 如果redis中没有数据直接返回空
        if (StringUtils.isBlank(jsonRedisData)) {
            return null;
        }

        // 2. 如果redis中有数据
        RedisData redisData = JSONUtil.toBean(jsonRedisData, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        T t = JSONUtil.toBean(data, type);
        // 3. 判断shop是否过期
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3.1 如果没过期，就直接返回数据
        if (expireTime.isAfter(LocalDateTime.now())) {
            return t;
        }
        String lockKey = lockKeyPrefix + id;
        if (lock(lockKey)) {
            // 3.2 获取锁之后要重新检查缓存中的数据有没有过期
            jsonRedisData = stringRedisTemplate.opsForValue().get(key);
            redisData = JSONUtil.toBean(jsonRedisData, RedisData.class);
            expireTime = redisData.getExpireTime();
            // 3.2.1 如果没过期
            if (expireTime.isAfter(LocalDateTime.now())) {
                return t;
            }
            // 3.2.2 如果过期，创建一个新的线程去重建缓存，自己先返回旧数据
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                // 3.2.2.1 重建缓存
                try {
                    T t1 = dbCallBack.apply(id);
                    this.setWithLogicalExpire(key,t1,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 4. 如果没抢到锁，就返回旧数据
        return t;
    }
    public boolean lock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "lock", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    public void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

}

