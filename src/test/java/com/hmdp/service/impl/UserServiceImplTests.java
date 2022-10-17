package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
/**
 * @Author FanJian
 * @Date 2022/10/16 16:24
 */

@SpringBootTest
public class UserServiceImplTests {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private UserServiceImpl userService;


    /**
     * 模拟创建一千个token并写入文件
     */
    @Test
    public void createTokenAndWriteFile() {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(new File("tokens.txt")));
            List<User> list = userService.list();
            for (User user : list) {
                LoginFormDTO loginFormDTO = new LoginFormDTO();
                loginFormDTO.setPhone(user.getPhone());
                String token = login(loginFormDTO);
                bos.write((token + "\n").getBytes());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        User user = userService.query().eq("phone", phone).one();
        String token = UUID.randomUUID().toString(true);

        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        // 将user信息存入redis中
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);

        // 设置user信息失效时间
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 将token返回给客户端
        return token;
    }
}
