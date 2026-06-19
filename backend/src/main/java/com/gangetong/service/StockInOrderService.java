package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.StockInOrderDTO;
import com.gangetong.entity.StockInOrder;

import java.util.List;

public interface StockInOrderService extends IService<StockInOrder> {

    List<StockInOrder> listAll();

    List<StockInOrder> listByStatus(String status);

    StockInOrder getDetailById(Long id);

    String generateOrderNo();

    boolean add(StockInOrderDTO dto);

    boolean audit(Long id, Long auditBy);

    boolean delete(Long id);
}
