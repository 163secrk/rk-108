package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.SteelSpec;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Override
    public List<Product> listAll() {
        List<Product> list = this.list();
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<Product> listByMaterialId(Long materialId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (materialId != null) {
            wrapper.eq(Product::getMaterialId, materialId);
        }
        List<Product> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<Long> materialIds = products.stream()
                .map(Product::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Material> materials = materialMapper.selectBatchIds(materialIds);
        Map<Long, String> materialNameMap = materials.stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        List<Long> specIds = products.stream()
                .map(Product::getSpecId)
                .distinct()
                .collect(Collectors.toList());
        List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
        Map<Long, SteelSpec> specMap = specs.stream()
                .collect(Collectors.toMap(SteelSpec::getId, s -> s));

        for (Product product : products) {
            product.setMaterialName(materialNameMap.get(product.getMaterialId()));
            SteelSpec spec = specMap.get(product.getSpecId());
            if (spec != null) {
                product.setDiameter(spec.getDiameter());
                product.setWallThickness(spec.getWallThickness());
                product.setLength(spec.getLength());
                product.setWeightPerMeter(spec.getWeightPerMeter());
            }
        }
    }

    @Override
    public Product getById(Long id) {
        Product product = super.getById(id);
        if (product != null) {
            if (product.getMaterialId() != null) {
                Material material = materialMapper.selectById(product.getMaterialId());
                if (material != null) {
                    product.setMaterialName(material.getName());
                }
            }
            if (product.getSpecId() != null) {
                SteelSpec spec = steelSpecMapper.selectById(product.getSpecId());
                if (spec != null) {
                    product.setDiameter(spec.getDiameter());
                    product.setWallThickness(spec.getWallThickness());
                    product.setLength(spec.getLength());
                    product.setWeightPerMeter(spec.getWeightPerMeter());
                }
            }
        }
        return product;
    }

    @Override
    public boolean add(Product product) {
        if (product.getCreateTime() == null || product.getCreateTime().isEmpty()) {
            product.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (product.getUnit() == null || product.getUnit().isEmpty()) {
            product.setUnit("支");
        }
        return this.save(product);
    }

    @Override
    public boolean update(Product product) {
        return this.updateById(product);
    }

    @Override
    public boolean delete(Long id) {
        return this.removeById(id);
    }

    @Override
    public boolean deleteByMaterialId(Long materialId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getMaterialId, materialId);
        return this.remove(wrapper);
    }
}
