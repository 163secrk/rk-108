package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.StockInItem;

import java.util.List;

public interface StockInItemService extends IService<StockInItem> {

    List<StockInItem> listByOrderId(Long orderId);

    boolean deleteByOrderId(Long orderId);
}
