package com.hmdp.service.impl;

import com.hmdp.entity.Shop;
import com.hmdp.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
/**
 * @Author FanJian
 * @Date 2022/10/16 16:27
 */
@SpringBootTest
public class ShopServiceImplTests {
    @Resource
    private ShopServiceImpl shopService;

    @Autowired
    private CacheClient cacheClient;

    @Test
    void testSetWithLogicalExpire() {
        Shop byId = shopService.getById(1);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1,byId,10L, TimeUnit.SECONDS);
    }
}
