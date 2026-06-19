package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.SalesOrder;
import com.gangetong.dto.SalesOrderDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface SalesOrderService extends IService<SalesOrder> {

    List<SalesOrder> listAll();

    List<SalesOrder> listByStatus(String status);

    SalesOrder getDetailById(Long id);

    String generateOrderNo();

    boolean add(SalesOrderDTO dto);

    boolean update(SalesOrderDTO dto);

    boolean delete(Long id);

    Map<String, Object> confirm(Long id);

    Map<String, Object> checkCredit(Long customerId, BigDecimal orderAmount);

    BigDecimal calculateCustomerDebt(Long customerId);
}
