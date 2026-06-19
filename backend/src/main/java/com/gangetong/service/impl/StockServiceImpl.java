package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.StockQueryDTO;
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
import com.gangetong.service.InventoryLockService;
import com.gangetong.service.StockService;
import com.gangetong.vo.StockBatchVO;
import com.gangetong.vo.StockSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private InventoryLockService inventoryLockService;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    @Transactional
    public boolean decreaseStock(Long stockId, Integer quantity, BigDecimal weight) {
        Stock stock = this.getById(stockId);
        if (stock == null) {
            throw new RuntimeException("库存记录不存在");
        }
        int currentQty = stock.getQuantity() != null ? stock.getQuantity() : 0;
        int outQty = quantity != null ? quantity : 0;
        if (outQty > currentQty) {
            throw new RuntimeException("库存不足，当前库存：" + currentQty + "，需要出库：" + outQty);
        }

        BigDecimal currentWeight = stock.getWeight() != null ? stock.getWeight() : BigDecimal.ZERO;
        BigDecimal outWeight = weight != null ? weight : BigDecimal.ZERO;
        if (outWeight.compareTo(currentWeight) > 0) {
            throw new RuntimeException("库存重量不足");
        }

        int newQty = currentQty - outQty;
        BigDecimal newWeight = currentWeight.subtract(outWeight);
        BigDecimal newCostAmount = BigDecimal.ZERO;
        BigDecimal newUnitPrice = stock.getCostUnitPrice() != null ? stock.getCostUnitPrice() : BigDecimal.ZERO;
        if (newQty > 0) {
            BigDecimal oldAmount = stock.getCostAmount() != null ? stock.getCostAmount() : BigDecimal.ZERO;
            BigDecimal outAmount = newUnitPrice.multiply(BigDecimal.valueOf(outQty));
            newCostAmount = oldAmount.subtract(outAmount);
        }

        stock.setQuantity(newQty);
        stock.setWeight(newWeight);
        stock.setCostAmount(newCostAmount);
        stock.setUpdateTime(now());
        return this.updateById(stock);
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

    @Override
    public List<StockSummaryVO> queryStockTree(StockQueryDTO dto) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Stock::getQuantity, 0);
        if (dto != null) {
            if (dto.getWarehouseId() != null) {
                wrapper.eq(Stock::getWarehouseId, dto.getWarehouseId());
            }
            if (dto.getMaterialId() != null) {
                wrapper.eq(Stock::getMaterialId, dto.getMaterialId());
            }
            if (dto.getSpecId() != null) {
                wrapper.eq(Stock::getSpecId, dto.getSpecId());
            }
        }
        wrapper.orderByDesc(Stock::getUpdateTime);
        List<Stock> stocks = this.list(wrapper);
        if (stocks == null || stocks.isEmpty()) {
            return new ArrayList<>();
        }

        fillRelatedData(stocks);

        Map<String, StockSummaryVO> summaryMap = new LinkedHashMap<>();
        long summaryId = 1L;

        for (Stock stock : stocks) {
            String key = stock.getWarehouseId() + "_" + stock.getSpecId();
            StockSummaryVO summary = summaryMap.get(key);
            if (summary == null) {
                summary = new StockSummaryVO();
                summary.setId(summaryId++ * 100000);
                summary.setWarehouseId(stock.getWarehouseId());
                summary.setWarehouseName(stock.getWarehouseName());
                summary.setMaterialId(stock.getMaterialId());
                summary.setMaterialName(stock.getMaterialName());
                summary.setSpecId(stock.getSpecId());
                summary.setSpecText(stock.getSpecText());
                summary.setProductId(stock.getProductId());
                summary.setProductName(stock.getProductName());
                summary.setTotalQuantity(0);
                summary.setTotalWeight(BigDecimal.ZERO);
                summary.setChildren(new ArrayList<>());
                summaryMap.put(key, summary);
            }

            summary.setTotalQuantity(summary.getTotalQuantity() + (stock.getQuantity() != null ? stock.getQuantity() : 0));
            summary.setTotalWeight(summary.getTotalWeight().add(stock.getWeight() != null ? stock.getWeight() : BigDecimal.ZERO));

            StockBatchVO batch = new StockBatchVO();
            batch.setId(stock.getId());
            batch.setFurnaceNo(stock.getFurnaceNo());
            batch.setRemainingQuantity(stock.getQuantity());
            batch.setRemainingWeight(stock.getWeight());
            batch.setUnitPrice(stock.getCostUnitPrice());

            String stockInDate = stockMapper.getStockInDate(stock.getProductId(), stock.getFurnaceNo());
            if (stockInDate != null && stockInDate.length() >= 10) {
                batch.setStockInDate(stockInDate.substring(0, 10));
            } else {
                String ct = stock.getCreateTime();
                if (ct != null && ct.length() >= 10) {
                    batch.setStockInDate(ct.substring(0, 10));
                }
            }

            summary.getChildren().add(batch);
        }

        return new ArrayList<>(summaryMap.values());
    }

    @Override
    public List<Stock> listAvailableForSale(Long productId, Long warehouseId, Long materialId, Long specId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.gt(Stock::getQuantity, 0);
        if (productId != null) {
            wrapper.eq(Stock::getProductId, productId);
        }
        if (warehouseId != null) {
            wrapper.eq(Stock::getWarehouseId, warehouseId);
        }
        if (materialId != null) {
            wrapper.eq(Stock::getMaterialId, materialId);
        }
        if (specId != null) {
            wrapper.eq(Stock::getSpecId, specId);
        }
        wrapper.orderByAsc(Stock::getCreateTime);
        List<Stock> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<StockSummaryVO> queryStockTreeForSale(Long warehouseId, Long materialId, Long specId, Long productId) {
        StockQueryDTO dto = new StockQueryDTO();
        dto.setWarehouseId(warehouseId);
        dto.setMaterialId(materialId);
        dto.setSpecId(specId);
        // queryStockTree内部不支持productId过滤，需要手动处理
        List<StockSummaryVO> tree = queryStockTree(dto);
        if (productId != null && tree != null) {
            tree = tree.stream()
                    .filter(s -> productId.equals(s.getProductId()))
                    .collect(Collectors.toList());
        }
        return tree != null ? tree : new ArrayList<>();
    }

    @Override
    public List<StockSummaryVO> queryStockTreeForSaleWithLock(Long warehouseId, Long materialId, Long specId, Long productId, Long excludeOrderId) {
        List<StockSummaryVO> tree = queryStockTreeForSale(warehouseId, materialId, specId, productId);
        if (tree == null || tree.isEmpty()) {
            return new ArrayList<>();
        }
        Long orderId = excludeOrderId != null ? excludeOrderId : -1L;
        for (StockSummaryVO summary : tree) {
            if (summary.getChildren() == null) continue;
            int totalAvailableQty = 0;
            BigDecimal totalAvailableWeight = BigDecimal.ZERO;

            for (StockBatchVO batch : summary.getChildren()) {
                int rawQty = batch.getRemainingQuantity() != null ? batch.getRemainingQuantity() : 0;
                if (rawQty <= 0) {
                    batch.setRemainingQuantity(0);
                    batch.setRemainingWeight(BigDecimal.ZERO);
                    continue;
                }
                // 通过stockId反查stock对象获取批次标识信息
                Stock stock = stockMapper.selectById(batch.getId());
                int available = rawQty;
                if (stock != null) {
                    available = inventoryLockService.getAvailableQuantityByBatch(
                            stock.getProductId(), stock.getWarehouseId(), stock.getFurnaceNo(), orderId);
                }
                available = Math.max(0, Math.min(available, rawQty));
                batch.setRemainingQuantity(available);

                BigDecimal rawWeight = batch.getRemainingWeight() != null ? batch.getRemainingWeight() : BigDecimal.ZERO;
                if (rawQty > 0 && available > 0) {
                    BigDecimal perQtyWeight = rawWeight.divide(BigDecimal.valueOf(rawQty), 6, BigDecimal.ROUND_HALF_UP);
                    BigDecimal availableWeight = perQtyWeight.multiply(BigDecimal.valueOf(available));
                    batch.setRemainingWeight(availableWeight);
                    totalAvailableWeight = totalAvailableWeight.add(availableWeight);
                } else {
                    batch.setRemainingWeight(BigDecimal.ZERO);
                }

                totalAvailableQty += available;
            }
            summary.setTotalQuantity(totalAvailableQty);
            summary.setTotalWeight(totalAvailableWeight);

            summary.setChildren(summary.getChildren().stream()
                    .filter(b -> b.getRemainingQuantity() != null && b.getRemainingQuantity() > 0)
                    .collect(Collectors.toList()));
        }
        return tree.stream()
                .filter(s -> s.getChildren() != null && !s.getChildren().isEmpty())
                .collect(Collectors.toList());
    }
}
