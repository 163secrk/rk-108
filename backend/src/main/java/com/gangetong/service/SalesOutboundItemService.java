package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.SalesOutboundItem;

import java.util.List;

public interface SalesOutboundItemService extends IService<SalesOutboundItem> {

    List<SalesOutboundItem> listByOutboundId(Long outboundId);

    boolean deleteByOutboundId(Long outboundId);
}
