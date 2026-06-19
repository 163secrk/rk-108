package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.SalesOrderItem;
import com.gangetong.mapper.SalesOrderItemMapper;
import com.gangetong.service.SalesOrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalesOrderItemServiceImpl extends ServiceImpl<SalesOrderItemMapper, SalesOrderItem> implements SalesOrderItemService {

    @Autowired
    private SalesOrderItemMapper salesOrderItemMapper;

    @Override
    public List<SalesOrderItem> listByOrderId(Long orderId) {
        LambdaQueryWrapper<SalesOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesOrderItem::getOrderId, orderId)
                .orderByAsc(SalesOrderItem::getSortNo);
        return this.list(wrapper);
    }

    @Override
    public void deleteByOrderId(Long orderId) {
        LambdaQueryWrapper<SalesOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesOrderItem::getOrderId, orderId);
        this.remove(wrapper);
    }
}
