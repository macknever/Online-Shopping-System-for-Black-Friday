package com.macknever.seckill.db.dao;

import com.macknever.seckill.db.po.Order;

public interface OrderDao {
    void insertOrder(Order order);
    Order queryOrder(String orderNo);
    void updateOrder(Order order);
}
