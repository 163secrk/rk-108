package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.Stock;
import com.gangetong.mapper.InventoryLockMapper;
import com.gangetong.mapper.StockMapper;
import com.gangetong.service.InventoryLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InventoryLockServiceImpl extends ServiceImpl<InventoryLockMapper, InventoryLock> implements InventoryLockService {

    @Autowired
    private InventoryLockMapper inventoryLockMapper;

    @Autowired
    private StockMapper stockMapper;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<InventoryLock> listByOrderId(Long orderId) {
        return inventoryLockMapper.listByOrderId(orderId);
    }

    @Override
    public List<InventoryLock> listByOrderItemId(Long orderItemId) {
        return inventoryLockMapper.listByOrderItemId(orderItemId);
    }

    @Override
    @Transactional
    public void releaseByOrderId(Long orderId) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryLock::getOrderId, orderId)
                .eq(InventoryLock::getStatus, "LOCKED");
        List<InventoryLock> locks = this.list(wrapper);
        for (InventoryLock lock : locks) {
            lock.setStatus("RELEASED");
            lock.setReleaseTime(now());
        }
        this.updateBatchById(locks);
    }

    @Override
    @Transactional
    public void lockStocks(Long orderId, Long orderItemId, List<Stock> stockBatches) {
        if (stockBatches == null || stockBatches.isEmpty()) {
            return;
        }
        for (Stock stock : stockBatches) {
            Integer qty = stock.getQuantity();
            if (qty == null || qty <= 0) {
                continue;
            }
            InventoryLock lock = new InventoryLock();
            lock.setOrderId(orderId);
            lock.setOrderItemId(orderItemId);
            lock.setStockId(stock.getId());
            lock.setProductId(stock.getProductId());
            lock.setWarehouseId(stock.getWarehouseId());
            lock.setFurnaceNo(stock.getFurnaceNo());
            lock.setLockQuantity(qty);
            lock.setLockWeight(stock.getWeight() != null ? stock.getWeight() : BigDecimal.ZERO);
            lock.setStatus("LOCKED");
            lock.setCreateTime(now());
            this.save(lock);
        }
    }

    @Override
    public int getAvailableQuantity(Long stockId) {
        Stock stock = stockMapper.selectById(stockId);
        if (stock == null || stock.getQuantity() == null) {
            return 0;
        }
        int totalQty = stock.getQuantity();
        Integer locked = inventoryLockMapper.sumLockedQuantityByStockId(stockId);
        return totalQty - (locked != null ? locked : 0);
    }

    @Override
    public int getAvailableQuantityByBatch(Long productId, Long warehouseId, String furnaceNo, Long excludeOrderId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProductId, productId)
                .eq(Stock::getWarehouseId, warehouseId)
                .eq(Stock::getFurnaceNo, furnaceNo);
        Stock stock = stockMapper.selectOne(wrapper);
        if (stock == null || stock.getQuantity() == null) {
            return 0;
        }
        int totalQty = stock.getQuantity();
        Long orderIdParam = excludeOrderId != null ? excludeOrderId : -1L;
        Integer locked = inventoryLockMapper.sumLockedQuantityByBatch(productId, warehouseId, furnaceNo, orderIdParam);
        return totalQty - (locked != null ? locked : 0);
    }
}
