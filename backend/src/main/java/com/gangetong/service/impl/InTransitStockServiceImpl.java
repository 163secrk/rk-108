package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.PurchaseContract;
import com.gangetong.entity.PurchaseContractItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.mapper.InTransitStockMapper;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.PurchaseContractMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.service.InTransitStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InTransitStockServiceImpl extends ServiceImpl<InTransitStockMapper, InTransitStock> implements InTransitStockService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private PurchaseContractMapper purchaseContractMapper;

    @Override
    public List<InTransitStock> listAll() {
        List<InTransitStock> list = this.list();
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<InTransitStock> listByProductId(Long productId) {
        LambdaQueryWrapper<InTransitStock> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            wrapper.eq(InTransitStock::getProductId, productId);
        }
        List<InTransitStock> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<InTransitStock> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> productIds = list.stream()
                .map(InTransitStock::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getProductName));

        List<Long> materialIds = list.stream()
                .map(InTransitStock::getMaterialId)
                .distinct()
                .collect(Collectors.toList());
        List<Material> materials = materialMapper.selectBatchIds(materialIds);
        Map<Long, String> materialNameMap = materials.stream()
                .collect(Collectors.toMap(Material::getId, Material::getName));

        List<Long> specIds = list.stream()
                .map(InTransitStock::getSpecId)
                .distinct()
                .collect(Collectors.toList());
        List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
        Map<Long, String> specTextMap = specs.stream()
                .collect(Collectors.toMap(SteelSpec::getId, s -> "φ" + s.getDiameter() + "×" + s.getWallThickness()));

        List<Long> contractIds = list.stream()
                .map(InTransitStock::getContractId)
                .distinct()
                .collect(Collectors.toList());
        List<PurchaseContract> contracts = purchaseContractMapper.selectBatchIds(contractIds);
        Map<Long, String> contractNoMap = contracts.stream()
                .collect(Collectors.toMap(PurchaseContract::getId, PurchaseContract::getContractNo));

        for (InTransitStock stock : list) {
            stock.setProductName(productNameMap.get(stock.getProductId()));
            stock.setMaterialName(materialNameMap.get(stock.getMaterialId()));
            stock.setSpecText(specTextMap.get(stock.getSpecId()));
            stock.setContractNo(contractNoMap.get(stock.getContractId()));
        }
    }

    @Override
    public boolean addInTransitStock(PurchaseContractItem item, Long contractId) {
        InTransitStock stock = new InTransitStock();
        stock.setProductId(item.getProductId());
        stock.setMaterialId(item.getMaterialId());
        stock.setSpecId(item.getSpecId());
        stock.setContractId(contractId);
        stock.setContractItemId(item.getId());
        stock.setQuantity(item.getQuantity());
        stock.setWeight(item.getWeight() != null ? item.getWeight() : BigDecimal.ZERO);
        return this.save(stock);
    }

    @Override
    public boolean releaseInTransitStock(Long contractItemId, Integer quantity) {
        LambdaQueryWrapper<InTransitStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InTransitStock::getContractItemId, contractItemId);
        InTransitStock stock = this.getOne(wrapper);
        if (stock == null) {
            return false;
        }
        int remaining = (stock.getQuantity() != null ? stock.getQuantity() : 0) - (quantity != null ? quantity : 0);
        if (remaining <= 0) {
            return this.removeById(stock.getId());
        } else {
            stock.setQuantity(remaining);
            if (stock.getWeight() != null && quantity != null) {
                BigDecimal unitWeight = stock.getWeight().divide(BigDecimal.valueOf(stock.getQuantity() + quantity), 3, BigDecimal.ROUND_HALF_UP);
                stock.setWeight(unitWeight.multiply(BigDecimal.valueOf(remaining)));
            }
            return this.updateById(stock);
        }
    }

    @Override
    public boolean deleteByContractId(Long contractId) {
        LambdaQueryWrapper<InTransitStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InTransitStock::getContractId, contractId);
        return this.remove(wrapper);
    }
}
