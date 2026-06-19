package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.Stock;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryLockService extends IService<InventoryLock> {

    List<InventoryLock> listByOrderId(Long orderId);

    List<InventoryLock> listByOrderItemId(Long orderItemId);

    void releaseByOrderId(Long orderId);

    void lockStocks(Long orderId, Long orderItemId, List<Stock> stockBatches);

    int getAvailableQuantity(Long stockId);

    int getAvailableQuantityByBatch(Long productId, Long warehouseId, String furnaceNo, Long excludeOrderId);
}
