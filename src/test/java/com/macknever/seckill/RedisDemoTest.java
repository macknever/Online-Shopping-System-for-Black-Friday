package com.macknever.seckill;

import com.macknever.seckill.util.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class RedisDemoTest {
    @Resource
    private RedisService redisService;

    @Test
    public void stockTest(){
        redisService.setValue("stock:18",100L);
    }
    @Test
    public void getStockTest() {
        String stock = redisService.getValue("stock:18");
        System.out.println(stock);
    }

    @Test
    public void stockDeductValidatorTest(){
        boolean result = redisService.stockDeductValidator("stock:18");
        System.out.println("result:"+result);
        String stock = redisService.getValue("stock:18");
        System.out.println("stock:"+stock);
    }
}
