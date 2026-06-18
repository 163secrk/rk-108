package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.PurchaseContractItem;

import java.util.List;

public interface PurchaseContractItemService extends IService<PurchaseContractItem> {

    List<PurchaseContractItem> listByContractId(Long contractId);

    boolean deleteByContractId(Long contractId);

    boolean deleteByContractIds(List<Long> contractIds);
}
