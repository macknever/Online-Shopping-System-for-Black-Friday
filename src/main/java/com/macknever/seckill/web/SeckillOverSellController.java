package com.macknever.seckill.web;

import com.macknever.seckill.services.SeckillActivityService;
import com.macknever.seckill.services.SeckillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeckillOverSellController {


    @Autowired
    private SeckillActivityService seckillActivityService;
    /**
     * 使用 lua 脚本处理抢购请求
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable long seckillActivityId) {
        boolean stockValidateResult =
                seckillActivityService.seckillStockValidator(seckillActivityId);
        return stockValidateResult ? "恭喜你秒杀成功" : "商品已经售完，下次再来";
    }

    @Autowired
    private SeckillOverSellService seckillOverSellService;

    /**
     * simply check
     *
     * @param seckillActivityId
     * @return
     */

    //@ResponseBody
    //@RequestMapping("seckill/{seckillActivityId}")
    public String seckill(@PathVariable long seckillActivityId) {
        return seckillOverSellService.processSeckill(seckillActivityId);
    }




}
