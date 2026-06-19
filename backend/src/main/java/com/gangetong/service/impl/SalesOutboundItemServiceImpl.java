package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.SalesOutboundItem;
import com.gangetong.mapper.SalesOutboundItemMapper;
import com.gangetong.service.SalesOutboundItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesOutboundItemServiceImpl extends ServiceImpl<SalesOutboundItemMapper, SalesOutboundItem> implements SalesOutboundItemService {

    @Override
    public List<SalesOutboundItem> listByOutboundId(Long outboundId) {
        return this.baseMapper.listByOutboundId(outboundId);
    }

    @Override
    public boolean deleteByOutboundId(Long outboundId) {
        LambdaQueryWrapper<SalesOutboundItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesOutboundItem::getOutboundId, outboundId);
        return this.remove(wrapper);
    }
}
