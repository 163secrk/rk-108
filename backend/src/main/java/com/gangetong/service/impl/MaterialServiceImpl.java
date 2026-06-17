package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.service.MaterialService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @Override
    public List<Material> listAll() {
        return this.list();
    }

    @Override
    public Material getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean add(Material material) {
        if (material.getCreateTime() == null || material.getCreateTime().isEmpty()) {
            material.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return this.save(material);
    }

    @Override
    public boolean update(Material material) {
        return this.updateById(material);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }
}
