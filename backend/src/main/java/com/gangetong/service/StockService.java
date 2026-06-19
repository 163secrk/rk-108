package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.Stock;

import java.math.BigDecimal;
import java.util.List;

public interface StockService extends IService<Stock> {

    List<Stock> listAll();

    List<Stock> listByProductId(Long productId);

    List<Stock> listByWarehouseId(Long warehouseId);

    boolean increaseStock(Long productId, Long materialId, Long specId, Long warehouseId,
                          String furnaceNo, Integer quantity, BigDecimal weight, BigDecimal costUnitPrice);
}
