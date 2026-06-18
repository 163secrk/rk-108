package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.PurchaseContractDTO;
import com.gangetong.entity.PurchaseContract;

import java.util.List;

public interface PurchaseContractService extends IService<PurchaseContract> {

    List<PurchaseContract> listAll();

    List<PurchaseContract> listByStatus(String status);

    PurchaseContract getDetailById(Long id);

    boolean add(PurchaseContractDTO dto);

    boolean update(PurchaseContractDTO dto);

    boolean delete(Long id);

    boolean audit(Long id);

    boolean complete(Long id);

    String generateContractNo();
}
