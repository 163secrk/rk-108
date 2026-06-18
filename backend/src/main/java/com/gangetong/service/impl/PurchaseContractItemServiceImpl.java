package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.PurchaseContractItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.PurchaseContractItemMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.service.PurchaseContractItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseContractItemServiceImpl extends ServiceImpl<PurchaseContractItemMapper, PurchaseContractItem> implements PurchaseContractItemService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Override
    public List<PurchaseContractItem> listByContractId(Long contractId) {
        LambdaQueryWrapper<PurchaseContractItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseContractItem::getContractId, contractId)
                .orderByAsc(PurchaseContractItem::getSortNo, PurchaseContractItem::getId);
        List<PurchaseContractItem> items = this.list(wrapper);
        fillRelatedData(items);
        return items;
    }

    private void fillRelatedData(List<PurchaseContractItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> productIds = items.stream()
                .map(PurchaseContractItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> materialIds = items.stream()
                .map(PurchaseContractItem::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Material> materials = materialMapper.selectBatchIds(materialIds);
        Map<Long, String> materialNameMap = materials.stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        List<Long> specIds = items.stream()
                .map(PurchaseContractItem::getSpecId)
                .distinct()
                .collect(Collectors.toList());
        List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
        Map<Long, SteelSpec> specMap = specs.stream()
                .collect(Collectors.toMap(SteelSpec::getId, s -> s));

        for (PurchaseContractItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                item.setProductCode(product.getProductCode());
                item.setProductName(product.getProductName());
            }
            item.setMaterialName(materialNameMap.get(item.getMaterialId()));
            SteelSpec spec = specMap.get(item.getSpecId());
            if (spec != null) {
                item.setDiameter(spec.getDiameter());
                item.setWallThickness(spec.getWallThickness());
                item.setLength(spec.getLength());
                item.setWeightPerMeter(spec.getWeightPerMeter());
            }
            item.setRemainingQuantity((item.getQuantity() != null ? item.getQuantity() : 0)
                    - (item.getInStockQuantity() != null ? item.getInStockQuantity() : 0));
        }
    }

    @Override
    public boolean deleteByContractId(Long contractId) {
        LambdaQueryWrapper<PurchaseContractItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseContractItem::getContractId, contractId);
        return this.remove(wrapper);
    }

    @Override
    public boolean deleteByContractIds(List<Long> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return true;
        }
        LambdaQueryWrapper<PurchaseContractItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurchaseContractItem::getContractId, contractIds);
        return this.remove(wrapper);
    }
}
