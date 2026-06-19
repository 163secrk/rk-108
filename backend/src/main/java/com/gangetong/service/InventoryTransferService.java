package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.InventoryTransferDTO;
import com.gangetong.entity.InventoryTransfer;
import com.gangetong.entity.InventoryTransferItem;

import java.util.List;

public interface InventoryTransferService extends IService<InventoryTransfer> {

    List<InventoryTransfer> listAll();

    List<InventoryTransfer> listByStatus(String status);

    List<InventoryTransfer> listByFromWarehouseId(Long warehouseId);

    List<InventoryTransfer> listByToWarehouseId(Long warehouseId);

    InventoryTransfer getDetailById(Long id);

    String generateTransferNo();

    boolean add(InventoryTransferDTO dto);

    boolean update(InventoryTransferDTO dto);

    boolean delete(Long id);

    boolean audit(Long id, Long userId);

    boolean receive(Long id, Long userId, List<InventoryTransferItem> items);
}
