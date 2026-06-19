package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.PurchaseContractItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.StockInItem;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.PurchaseContractItemMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.mapper.StockInItemMapper;
import com.gangetong.service.StockInItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockInItemServiceImpl extends ServiceImpl<StockInItemMapper, StockInItem> implements StockInItemService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private PurchaseContractItemMapper contractItemMapper;

    @Override
    public List<StockInItem> listByOrderId(Long orderId) {
        LambdaQueryWrapper<StockInItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInItem::getOrderId, orderId)
                .orderByAsc(StockInItem::getId);
        List<StockInItem> items = this.list(wrapper);
        fillRelatedData(items);
        return items;
    }

    private void fillRelatedData(List<StockInItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> productIds = items.stream()
                .map(StockInItem::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> materialIds = items.stream()
                .map(StockInItem::getMaterialId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> materialNameMap = new java.util.HashMap<>();
        if (!materialIds.isEmpty()) {
            List<Material> materials = materialMapper.selectBatchIds(materialIds);
            materialNameMap = materials.stream()
                    .collect(Collectors.toMap(Material::getId, Material::getName));
        }

        List<Long> specIds = items.stream()
                .map(StockInItem::getSpecId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SteelSpec> specMap = new java.util.HashMap<>();
        if (!specIds.isEmpty()) {
            List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
            specMap = specs.stream()
                    .collect(Collectors.toMap(SteelSpec::getId, s -> s));
        }

        List<Long> contractItemIds = items.stream()
                .map(StockInItem::getContractItemId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PurchaseContractItem> contractItemMap = new java.util.HashMap<>();
        if (!contractItemIds.isEmpty()) {
            List<PurchaseContractItem> contractItems = contractItemMapper.selectBatchIds(contractItemIds);
            contractItemMap = contractItems.stream()
                    .collect(Collectors.toMap(PurchaseContractItem::getId, c -> c));
        }

        for (StockInItem item : items) {
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

            PurchaseContractItem contractItem = contractItemMap.get(item.getContractItemId());
            if (contractItem != null) {
                item.setContractQuantity(contractItem.getQuantity());
                item.setInStockQuantity(contractItem.getInStockQuantity());
                item.setContractWeight(contractItem.getWeight());
                int remaining = (contractItem.getQuantity() != null ? contractItem.getQuantity() : 0)
                        - (contractItem.getInStockQuantity() != null ? contractItem.getInStockQuantity() : 0);
                item.setRemainingQuantity(remaining);
            }
        }
    }

    @Override
    public boolean deleteByOrderId(Long orderId) {
        LambdaQueryWrapper<StockInItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInItem::getOrderId, orderId);
        return this.remove(wrapper);
    }
}
