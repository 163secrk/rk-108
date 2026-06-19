package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Material;
import com.gangetong.entity.Product;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.Stock;
import com.gangetong.entity.Warehouse;
import com.gangetong.mapper.MaterialMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.mapper.StockMapper;
import com.gangetong.mapper.WarehouseMapper;
import com.gangetong.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<Stock> listAll() {
        List<Stock> list = this.list(new LambdaQueryWrapper<Stock>().orderByDesc(Stock::getUpdateTime));
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<Stock> listByProductId(Long productId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        if (productId != null) {
            wrapper.eq(Stock::getProductId, productId);
        }
        wrapper.orderByDesc(Stock::getUpdateTime);
        List<Stock> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<Stock> listByWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(Stock::getWarehouseId, warehouseId);
        }
        wrapper.orderByDesc(Stock::getUpdateTime);
        List<Stock> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<Stock> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> productIds = list.stream()
                .map(Stock::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, String> productNameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getProductName));

        List<Long> materialIds = list.stream()
                .map(Stock::getMaterialId)
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
                .map(Stock::getSpecId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> specTextMap = new HashMap<>();
        if (!specIds.isEmpty()) {
            List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
            specTextMap = specs.stream()
                    .collect(Collectors.toMap(SteelSpec::getId, s -> "φ" + s.getDiameter() + "×" + s.getWallThickness()));
        }

        List<Long> warehouseIds = list.stream()
                .map(Stock::getWarehouseId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<Warehouse> warehouses = warehouseMapper.selectBatchIds(warehouseIds);
            warehouseNameMap = warehouses.stream()
                    .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        }

        for (Stock stock : list) {
            stock.setProductName(productNameMap.get(stock.getProductId()));
            stock.setMaterialName(materialNameMap.get(stock.getMaterialId()));
            stock.setSpecText(specTextMap.get(stock.getSpecId()));
            stock.setWarehouseName(warehouseNameMap.get(stock.getWarehouseId()));
        }
    }

    @Override
    @Transactional
    public boolean increaseStock(Long productId, Long materialId, Long specId, Long warehouseId,
                                 String furnaceNo, Integer quantity, BigDecimal weight, BigDecimal costUnitPrice) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProductId, productId)
                .eq(Stock::getWarehouseId, warehouseId)
                .eq(Stock::getFurnaceNo, furnaceNo);
        Stock stock = this.getOne(wrapper);

        BigDecimal inWeight = weight != null ? weight : BigDecimal.ZERO;
        BigDecimal inUnitPrice = costUnitPrice != null ? costUnitPrice : BigDecimal.ZERO;
        BigDecimal inCostAmount = inUnitPrice.multiply(BigDecimal.valueOf(quantity));

        if (stock == null) {
            stock = new Stock();
            stock.setProductId(productId);
            stock.setMaterialId(materialId);
            stock.setSpecId(specId);
            stock.setWarehouseId(warehouseId);
            stock.setFurnaceNo(furnaceNo);
            stock.setQuantity(quantity);
            stock.setWeight(inWeight);
            stock.setCostUnitPrice(inUnitPrice);
            stock.setCostAmount(inCostAmount);
            stock.setCreateTime(now());
            stock.setUpdateTime(now());
            return this.save(stock);
        }

        int newQty = (stock.getQuantity() != null ? stock.getQuantity() : 0) + quantity;
        BigDecimal newWeight = (stock.getWeight() != null ? stock.getWeight() : BigDecimal.ZERO).add(inWeight);
        BigDecimal oldAmount = stock.getCostAmount() != null ? stock.getCostAmount() : BigDecimal.ZERO;
        BigDecimal totalAmount = oldAmount.add(inCostAmount);
        BigDecimal avgUnitPrice = newQty > 0
                ? totalAmount.divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP)
                : inUnitPrice;

        stock.setQuantity(newQty);
        stock.setWeight(newWeight);
        stock.setCostUnitPrice(avgUnitPrice);
        stock.setCostAmount(totalAmount);
        stock.setUpdateTime(now());
        return this.updateById(stock);
    }
}
