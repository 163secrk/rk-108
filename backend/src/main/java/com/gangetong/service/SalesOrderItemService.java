package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.SalesOrderItem;

import java.util.List;

public interface SalesOrderItemService extends IService<SalesOrderItem> {

    List<SalesOrderItem> listByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
