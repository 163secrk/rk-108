package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.InventoryTransferItem;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.SteelSpec;
import com.gangetong.mapper.InventoryTransferItemMapper;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.service.InventoryTransferItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryTransferItemServiceImpl extends ServiceImpl<InventoryTransferItemMapper, InventoryTransferItem> implements InventoryTransferItemService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Override
    public List<InventoryTransferItem> listByTransferId(Long transferId) {
        LambdaQueryWrapper<InventoryTransferItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryTransferItem::getTransferId, transferId);
        wrapper.orderByAsc(InventoryTransferItem::getSortNo);
        return this.list(wrapper);
    }

    @Override
    public List<InventoryTransferItem> listByTransferIdWithDetail(Long transferId) {
        List<InventoryTransferItem> list = listByTransferId(transferId);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<InventoryTransferItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> productIds = list.stream()
                .map(InventoryTransferItem::getProductId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> productNameMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIds);
            productNameMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, Product::getProductName));
        }

        List<Long> materialIds = list.stream()
                .map(InventoryTransferItem::getMaterialId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> materialNameMap = new HashMap<>();
        if (!materialIds.isEmpty()) {
            List<Material> materials = materialMapper.selectBatchIds(materialIds);
            materialNameMap = materials.stream()
                    .collect(Collectors.toMap(Material::getId, Material::getName));
        }

        List<Long> specIds = list.stream()
                .map(InventoryTransferItem::getSpecId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> specTextMap = new HashMap<>();
        if (!specIds.isEmpty()) {
            List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
            specTextMap = specs.stream()
                    .collect(Collectors.toMap(SteelSpec::getId, s -> "φ" + s.getDiameter() + "×" + s.getWallThickness()));
        }

        for (InventoryTransferItem item : list) {
            item.setProductName(productNameMap.get(item.getProductId()));
            item.setMaterialName(materialNameMap.get(item.getMaterialId()));
            item.setSpecText(specTextMap.get(item.getSpecId()));
        }
    }

    @Override
    public boolean deleteByTransferId(Long transferId) {
        LambdaQueryWrapper<InventoryTransferItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryTransferItem::getTransferId, transferId);
        return this.remove(wrapper);
    }
}
