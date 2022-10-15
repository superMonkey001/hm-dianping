package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    @Autowired
    private CacheClient cacheClient;

    @Test
    void test() {
        Shop byId = shopService.getById(1);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1,byId,10L, TimeUnit.SECONDS);
    }

    @Autowired
    private RedisIdWorker redisIdWorker;

    private static final ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id : " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        // 等到剩余任务数为零
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println(end - begin);

    }


    public String test1() {
        try{
            System.out.println("try");
            return "return";
        } catch (Exception e) {

        } finally {
            System.out.println("final");
        }
        return null;
    }
    public void test2() {
        String result = test1();
        System.out.println(result);
    }
}
