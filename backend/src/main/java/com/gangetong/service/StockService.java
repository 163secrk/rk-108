package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.StockQueryDTO;
import com.gangetong.entity.Stock;
import com.gangetong.vo.StockSummaryVO;

import java.math.BigDecimal;
import java.util.List;

public interface StockService extends IService<Stock> {

    List<Stock> listAll();

    List<Stock> listByProductId(Long productId);

    List<Stock> listByWarehouseId(Long warehouseId);

    List<Stock> listByWarehouseIdWithChildren(Long warehouseId);

    boolean increaseStock(Long productId, Long materialId, Long specId, Long warehouseId,
                          String furnaceNo, Integer quantity, BigDecimal weight, BigDecimal costUnitPrice);

    boolean decreaseStock(Long stockId, Integer quantity, BigDecimal weight);

    List<StockSummaryVO> queryStockTree(StockQueryDTO dto);

    List<Stock> listAvailableForSale(Long productId, Long warehouseId, Long materialId, Long specId);

    List<StockSummaryVO> queryStockTreeForSale(Long warehouseId, Long materialId, Long specId, Long productId);

    List<StockSummaryVO> queryStockTreeForSaleWithLock(Long warehouseId, Long materialId, Long specId, Long productId, Long excludeOrderId);
}
