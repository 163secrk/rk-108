package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.InventoryTransferItem;
import com.gangetong.entity.PurchaseContractItem;

import java.util.List;

public interface InTransitStockService extends IService<InTransitStock> {

    List<InTransitStock> listAll();

    List<InTransitStock> listByProductId(Long productId);

    List<InTransitStock> listByTransferId(Long transferId);

    List<InTransitStock> listByToWarehouseId(Long warehouseId);

    boolean addInTransitStock(PurchaseContractItem item, Long contractId);

    boolean addTransferInTransitStock(InventoryTransferItem item, Long transferId, Long fromWarehouseId, Long toWarehouseId);

    boolean releaseInTransitStock(Long contractItemId, Integer quantity);

    boolean releaseTransferInTransitStock(Long transferItemId, Integer quantity);

    boolean deleteByContractId(Long contractId);

    boolean deleteByTransferId(Long transferId);
}
