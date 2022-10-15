package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author FanJian
 * @Date 2022/10/14 13:33
 */
@Data
public class RedisData {
    LocalDateTime expireTime;
    Object data;
}
