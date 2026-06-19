package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.InventoryTransferItem;

import java.util.List;

public interface InventoryTransferItemService extends IService<InventoryTransferItem> {

    List<InventoryTransferItem> listByTransferId(Long transferId);

    List<InventoryTransferItem> listByTransferIdWithDetail(Long transferId);

    boolean deleteByTransferId(Long transferId);
}
