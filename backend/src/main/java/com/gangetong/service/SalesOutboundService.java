package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.dto.SalesOutboundDTO;
import com.gangetong.entity.SalesOutbound;

import java.util.List;

public interface SalesOutboundService extends IService<SalesOutbound> {

    List<SalesOutbound> listAll();

    List<SalesOutbound> listByStatus(String status);

    SalesOutbound getDetailById(Long id);

    String generateOutboundNo();

    boolean add(SalesOutboundDTO dto);

    boolean update(SalesOutboundDTO dto);

    boolean delete(Long id);

    boolean audit(Long id, Long auditBy);

    SalesOutbound generateFromOrder(Long orderId);
}
