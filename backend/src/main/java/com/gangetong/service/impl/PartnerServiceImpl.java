package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Partner;
import com.gangetong.mapper.PartnerMapper;
import com.gangetong.service.PartnerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PartnerServiceImpl extends ServiceImpl<PartnerMapper, Partner> implements PartnerService {

    @Override
    public List<Partner> listAll() {
        return this.list(new LambdaQueryWrapper<Partner>().orderByDesc(Partner::getCreateTime));
    }

    @Override
    public List<Partner> listByType(String type) {
        return this.list(new LambdaQueryWrapper<Partner>()
                .eq(Partner::getType, type)
                .orderByDesc(Partner::getCreateTime));
    }

    @Override
    public Partner getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean add(Partner partner) {
        if (partner.getCreateTime() == null || partner.getCreateTime().isEmpty()) {
            partner.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (partner.getStatus() == null) {
            partner.setStatus(1);
        }
        return this.save(partner);
    }

    @Override
    public boolean update(Partner partner) {
        return this.updateById(partner);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }
}
