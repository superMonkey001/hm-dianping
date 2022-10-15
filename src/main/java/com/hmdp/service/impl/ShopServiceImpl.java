package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.dto.RedisData;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;


    @Autowired
    private CacheClient cacheClient;
    @Override
    public Result queryById(Long id) {
        /// 解决缓存穿透的代码
        // Shop shop = passThrough(id);
        // Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        Shop shop = cacheClient.queryWithLogical(CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        if (shop == null)  {
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);
    }

/*
    private Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        String jsonShop = redisTemplate.opsForValue().get(key);

        if (StringUtils.isNotBlank(jsonShop)) {
            Shop shop = JSONUtil.toBean(jsonShop, Shop.class);
            return shop;
        }

        // 如果redis中的数据为""
        if (jsonShop != null) {
            return null;
        }
        Shop shop = null;
        try {
            /// 否则让一个请求去查数据库
            // 如果没有抢到锁，那么就休眠一会，休眠完后重新请求这个方法。
            if (!lock(LOCK_SHOP_KEY)) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 获取锁成功再次检测redis缓存是否存在，做DoubleCheck，如果存在则无需重建缓存
            jsonShop = redisTemplate.opsForValue().get(key);
            if (StringUtils.isNotBlank(jsonShop)) {
                shop = JSONUtil.toBean(jsonShop, Shop.class);
                return shop;
            }
            if (jsonShop != null) {
                return null;
            }

            shop = getById(id);
            // 模拟重建缓存的时间
            Thread.sleep(200);
            if (shop == null) {
                redisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);
            } else {
                redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(LOCK_SHOP_KEY);
        }
        return shop;
    }
*/






    /**
     * 为了保证缓存一致性，要先操作数据库，然后再删除缓存
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        // 鲁棒性
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 操作数据库
        updateById(shop);
        // 删除缓存
        redisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
