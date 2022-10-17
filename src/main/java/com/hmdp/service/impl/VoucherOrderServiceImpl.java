package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());
        int res = result.intValue();
        if (res != 0) {
            return Result.fail(res == 1 ? "库存不足" : "不能重复下单");
        }
        long orderId = redisIdWorker.nextId("order");
        return Result.ok(orderId);
    }



    /**
     * 涉及多表操作，需加上@Transactional注解
     * @param voucherId
     * @return
     */
/*
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 秒杀券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 判断券的可以使用的开始时间是否还没到
        if (LocalDateTime.now().isBefore(voucher.getBeginTime())) {
            return Result.fail("秒杀尚未开始");
        }
        if (LocalDateTime.now().isAfter(voucher.getEndTime())) {
            return Result.fail("秒杀已经结束");
        }

        Integer stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();

        // SimpleRedisLock redisLock = new SimpleRedisLock("order" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        boolean isLock = lock.tryLock();
        if (!isLock) {
            return Result.fail("不允许多次下单");
        }

        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(userId,voucherId);
        } finally {
            lock.unlock();
        }
    }
*/

    @Transactional(rollbackFor = Exception.class)
    public Result createVoucherOrder(Long userId,Long voucherId) {
        /// 自己的写法
        // VoucherOrder order = getOne((Wrapper<VoucherOrder>) new QueryWrapper().eq("userId", userId)).eq("voucher_id",voucherId);
        // if (order != null) {
        //     return Result.fail("您已经抢过该商品啦");
        // }
        // 更快的写法,查询用户有没有秒杀过
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("您已经抢过该商品啦");
        }


        // 更新seckillVoucher秒杀优惠券表
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherId).gt("stock",0).update();
        if (!success) {
            return Result.fail("库存不足");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        // 向全局优惠券订单表中增加订单
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
