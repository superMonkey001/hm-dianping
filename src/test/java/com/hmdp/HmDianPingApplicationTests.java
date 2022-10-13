package com.hmdp;

import cn.hutool.core.io.FastByteArrayOutputStream;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HmDianPingApplicationTests {

    public static void main(String[] args) {
        new HmDianPingApplicationTests().test2();
    }
    public String test() {
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
        String result = test();
        System.out.println(result);
    }
}
