package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.PurchaseContractItem;

import java.util.List;

public interface InTransitStockService extends IService<InTransitStock> {

    List<InTransitStock> listAll();

    List<InTransitStock> listByProductId(Long productId);

    boolean addInTransitStock(PurchaseContractItem item, Long contractId);

    boolean releaseInTransitStock(Long contractItemId, Integer quantity);

    boolean deleteByContractId(Long contractId);
}
