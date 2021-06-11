package com.macknever.seckill.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.macknever.seckill.db.dao.OrderDao;
import com.macknever.seckill.db.dao.SeckillActivityDao;
import com.macknever.seckill.db.dao.SeckillCommodityDao;
import com.macknever.seckill.db.po.Order;
import com.macknever.seckill.db.po.SeckillActivity;
import com.macknever.seckill.db.po.SeckillCommodity;
import com.macknever.seckill.services.SeckillActivityService;
import com.macknever.seckill.util.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class SeckillActivityController {

    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @ResponseBody
    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap
    ) throws ParseException {
        startTime = startTime.substring(0, 10) + startTime.substring(11);
        endTime = endTime.substring(0, 10) + endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(new Integer("" + seckillNumber));
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));
        seckillActivityDao.inertSeckillActivity(seckillActivity);
        resultMap.put("seckillActivity", seckillActivity);
        return "add_success";
    }

    @RequestMapping("/seckills")
    public String activityList(Map<String, Object> resultMap) {
        try (Entry entry = SphU.entry("seckills")) {
            List<SeckillActivity> seckillActivities =
                    seckillActivityDao.querySeckillActivitysByStatus(1);
            resultMap.put("seckillActivities", seckillActivities);
            return "seckill_activity";
        } catch (BlockException ex) {
            log.error("查询秒杀活动的列表被限流 "+ex.toString());
            return "wait";
        }
    }

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;
    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(Map<String, Object> resultMap, @PathVariable long
            seckillActivityId) {
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;
        String seckillActivityInfo = redisService.getValue("seckillActivity:" +
                seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityInfo)) {
            log.info("redis缓存数据:" + seckillActivityInfo);
            seckillActivity = JSON.parseObject(seckillActivityInfo,
                    SeckillActivity.class);
        } else {
            seckillActivity =
                    seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }
        String seckillCommodityInfo = redisService.getValue("seckillCommodity:"
                + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityInfo)) {
            log.info("redis缓存数据:" + seckillCommodityInfo);
            seckillCommodity = JSON.parseObject(seckillActivityInfo,
                    SeckillCommodity.class);
        } else {
            seckillCommodity =
                    seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        }
        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);
        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice", seckillActivity.getOldPrice());
        resultMap.put("commodityId", seckillActivity.getCommodityId());
        resultMap.put("commodityName", seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());
        return "seckill_item";
    }

    @Autowired
    SeckillActivityService seckillActivityService;

    @Autowired
    private RedisService redisService;

    /**
     * 处理抢购请求
     * @param userId
     * @param seckillActivityId
     * @return
     */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable long userId,
                                         @PathVariable long seckillActivityId) {
        boolean stockValidateResult = false;
        ModelAndView modelAndView = new ModelAndView();




        try {
            /*
            * check if user is already in the list
             */

//            if (redisService.isInLimitMember(seckillActivityId,userId)) {
//                modelAndView.addObject("resultInfo","sorry, you have already bought one");
//                modelAndView.setViewName("seckill_result");
//
//                return modelAndView;
//            }
            /*
             * 确认是否能够进行秒杀
             */
            stockValidateResult =
                    seckillActivityService.seckillStockValidator(seckillActivityId);
            if (stockValidateResult) {
                Order order =
                        seckillActivityService.createOrder(seckillActivityId, userId);
                modelAndView.addObject("resultInfo","秒杀成功，订单创建中，订单ID："
                        + order.getOrderNo());
                modelAndView.addObject("orderNo",order.getOrderNo());

                // add user to limitedMember
                redisService.addLimitMember(seckillActivityId,userId);
            } else {
                modelAndView.addObject("resultInfo","对不起，商品库存不足");
            }
        } catch (Exception e) {
            log.error("秒杀系统异常" + e.toString());
            modelAndView.addObject("resultInfo","秒杀失败");
        }
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    @Autowired
    OrderDao orderDao;

    /**
     * 订单查询
     * @param orderNo
     * @return
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(@PathVariable String orderNo) {
        log.info("订单查询，订单号：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();
        if (order != null) {
            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity =
                    seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order_wait");
        }

        return modelAndView;
    }

    /**
     * 订单支付
     * @return
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(@PathVariable String orderNo) throws Exception {
        seckillActivityService.payOrderProcess(orderNo);
        return "redirect:/seckill/orderQuery/" + orderNo;
    }

    /**
     * 获取当前服务器端的时间
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/getSystemTime")
    public String getSystemTime() {
//设置日期格式
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
// new Date()为获取当前系统时间
        String date = df.format(new Date());
        return date;
    }
}

