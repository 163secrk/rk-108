package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.entity.SteelSpec;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.service.MaterialService;
import com.gangetong.service.SteelSpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SteelSpecServiceImpl extends ServiceImpl<SteelSpecMapper, SteelSpec> implements SteelSpecService {

    @Autowired
    private MaterialMapper materialMapper;

    @Override
    public List<SteelSpec> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<SteelSpec> wrapper = new LambdaQueryWrapper<>();
        if (materialId != null) {
            wrapper.eq(SteelSpec::getMaterialId, materialId);
        }
        List<SteelSpec> list = this.list(wrapper);
        fillMaterialName(list);
        return list;
    }

    @Override
    public List<SteelSpec> listAll() {
        List<SteelSpec> list = this.list();
        fillMaterialName(list);
        return list;
    }

    private void fillMaterialName(List<SteelSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        List<Long> materialIds = specs.stream()
                .map(SteelSpec::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Material> materials = materialMapper.selectBatchIds(materialIds);
        Map<Long, String> materialNameMap = materials.stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));
        for (SteelSpec spec : specs) {
            spec.setMaterialName(materialNameMap.get(spec.getMaterialId()));
        }
    }

    @Override
    public SteelSpec getById(Long id) {
        SteelSpec spec = super.getById(id);
        if (spec != null && spec.getMaterialId() != null) {
            Material material = materialMapper.selectById(spec.getMaterialId());
            if (material != null) {
                spec.setMaterialName(material.getName());
            }
        }
        return spec;
    }

    @Override
    public boolean add(SteelSpec spec) {
        if (spec.getCreateTime() == null || spec.getCreateTime().isEmpty()) {
            spec.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        return this.save(spec);
    }

    @Override
    public boolean update(SteelSpec spec) {
        return this.updateById(spec);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean deleteByMaterialId(Long materialId) {
        LambdaQueryWrapper<SteelSpec> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SteelSpec::getMaterialId, materialId);
        return this.remove(wrapper);
    }
}
