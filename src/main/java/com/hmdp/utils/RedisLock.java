package com.hmdp.utils;

/**
 * @Author FanJian
 * @Date 2022/10/15 18:33
 */


public interface RedisLock {
    boolean tryLock(long timeoutSec);
    void unlock();
}
