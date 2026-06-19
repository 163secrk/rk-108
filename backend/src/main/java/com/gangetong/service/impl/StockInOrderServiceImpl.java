package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.StockInOrderDTO;
import com.gangetong.entity.Partner;
import com.gangetong.entity.PurchaseContract;
import com.gangetong.entity.PurchaseContractItem;
import com.gangetong.entity.StockInItem;
import com.gangetong.entity.StockInOrder;
import com.gangetong.entity.User;
import com.gangetong.entity.Warehouse;
import com.gangetong.mapper.PartnerMapper;
import com.gangetong.mapper.PurchaseContractMapper;
import com.gangetong.mapper.StockInOrderMapper;
import com.gangetong.mapper.UserMapper;
import com.gangetong.mapper.WarehouseMapper;
import com.gangetong.service.InTransitStockService;
import com.gangetong.service.PurchaseContractItemService;
import com.gangetong.service.PurchaseContractService;
import com.gangetong.service.StockInItemService;
import com.gangetong.service.StockInOrderService;
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
public class StockInOrderServiceImpl extends ServiceImpl<StockInOrderMapper, StockInOrder> implements StockInOrderService {

    @Autowired
    private PurchaseContractService purchaseContractService;

    @Autowired
    private PurchaseContractItemService contractItemService;

    @Autowired
    private StockInItemService stockInItemService;

    @Autowired
    private StockService stockService;

    @Autowired
    private InTransitStockService inTransitStockService;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PurchaseContractMapper purchaseContractMapper;

    private static final Map<String, String> STATUS_TEXT_MAP = new HashMap<>();

    static {
        STATUS_TEXT_MAP.put("DRAFT", "待审核");
        STATUS_TEXT_MAP.put("AUDITED", "已审核");
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<StockInOrder> listAll() {
        List<StockInOrder> list = this.list(new LambdaQueryWrapper<StockInOrder>()
                .orderByDesc(StockInOrder::getCreateTime));
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<StockInOrder> listByStatus(String status) {
        LambdaQueryWrapper<StockInOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(StockInOrder::getStatus, status);
        }
        wrapper.orderByDesc(StockInOrder::getCreateTime);
        List<StockInOrder> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<StockInOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<Long> contractIds = orders.stream()
                .map(StockInOrder::getContractId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> contractNoMap = new HashMap<>();
        if (!contractIds.isEmpty()) {
            List<PurchaseContract> contracts = purchaseContractMapper.selectBatchIds(contractIds);
            contractNoMap = contracts.stream()
                    .collect(Collectors.toMap(PurchaseContract::getId, PurchaseContract::getContractNo));
        }

        List<Long> supplierIds = orders.stream()
                .map(StockInOrder::getSupplierId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> supplierNameMap = new HashMap<>();
        if (!supplierIds.isEmpty()) {
            List<Partner> suppliers = partnerMapper.selectBatchIds(supplierIds);
            supplierNameMap = suppliers.stream()
                    .collect(Collectors.toMap(Partner::getId, Partner::getName));
        }

        List<Long> warehouseIds = orders.stream()
                .map(StockInOrder::getWarehouseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<Warehouse> warehouses = warehouseMapper.selectBatchIds(warehouseIds);
            warehouseNameMap = warehouses.stream()
                    .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        }

        List<Long> userIds = orders.stream()
                .flatMap(o -> List.of(
                        o.getCreateBy() != null ? o.getCreateBy() : -1L,
                        o.getAuditBy() != null ? o.getAuditBy() : -1L).stream())
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname));
        }

        for (StockInOrder order : orders) {
            order.setContractNo(contractNoMap.get(order.getContractId()));
            order.setSupplierName(supplierNameMap.get(order.getSupplierId()));
            order.setWarehouseName(warehouseNameMap.get(order.getWarehouseId()));
            order.setStatusText(STATUS_TEXT_MAP.get(order.getStatus()));
            if (order.getCreateBy() != null) {
                order.setCreateByName(userNameMap.get(order.getCreateBy()));
            }
            if (order.getAuditBy() != null) {
                order.setAuditByName(userNameMap.get(order.getAuditBy()));
            }
        }
    }

    @Override
    public StockInOrder getDetailById(Long id) {
        StockInOrder order = this.getById(id);
        if (order != null) {
            List<StockInOrder> wrapper = java.util.Collections.singletonList(order);
            fillRelatedData(wrapper);
            List<StockInItem> items = stockInItemService.listByOrderId(id);
            order.setItems(items);
        }
        return order;
    }

    @Override
    public String generateOrderNo() {
        String prefix = "RK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<StockInOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StockInOrder::getOrderNo, prefix + "%")
                .orderByDesc(StockInOrder::getOrderNo)
                .last("limit 1");
        StockInOrder last = this.getOne(wrapper);

        int sequence = 1;
        if (last != null && last.getOrderNo() != null) {
            try {
                String seqStr = last.getOrderNo().substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    @Override
    @Transactional
    public boolean add(StockInOrderDTO dto) {
        PurchaseContract contract = purchaseContractService.getById(dto.getContractId());
        if (contract == null) {
            throw new RuntimeException("采购合同不存在");
        }
        if (!"AUDITED".equals(contract.getStatus())) {
            throw new RuntimeException("只能对已审核状态的采购合同进行入库");
        }
        if (dto.getWarehouseId() == null) {
            throw new RuntimeException("请选择入库仓库");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("入库明细不能为空");
        }

        validateItems(dto.getItems());

        StockInOrder order = new StockInOrder();
        order.setOrderNo(dto.getOrderNo() != null ? dto.getOrderNo() : generateOrderNo());
        order.setContractId(dto.getContractId());
        order.setSupplierId(contract.getSupplierId());
        order.setWarehouseId(dto.getWarehouseId());
        order.setStatus("DRAFT");
        order.setRemark(dto.getRemark());
        order.setCreateBy(dto.getCreateBy());
        order.setCreateTime(now());
        order.setUpdateTime(now());

        boolean saved = this.save(order);
        if (!saved) {
            return false;
        }

        saveOrderItems(order.getId(), dto.getItems());
        recalculateOrderTotals(order.getId());
        return true;
    }

    private void validateItems(List<StockInItem> items) {
        List<StockInItem> validItems = items.stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .collect(Collectors.toList());
        if (validItems.isEmpty()) {
            throw new RuntimeException("请至少录入一条有效的入库明细");
        }

        List<Long> contractItemIds = validItems.stream()
                .map(StockInItem::getContractItemId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PurchaseContractItem> contractItemMap = new HashMap<>();
        if (!contractItemIds.isEmpty()) {
            List<PurchaseContractItem> contractItems = contractItemService.listByIds(contractItemIds);
            contractItemMap = contractItems.stream()
                    .collect(Collectors.toMap(PurchaseContractItem::getId, c -> c));
        }

        for (StockInItem item : validItems) {
            if (item.getFurnaceNo() == null || item.getFurnaceNo().trim().isEmpty()) {
                throw new RuntimeException("每条入库明细必须录入炉批号");
            }
            if (item.getActualWeight() == null || item.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("每条入库明细必须录入过磅重量");
            }
            PurchaseContractItem contractItem = contractItemMap.get(item.getContractItemId());
            if (contractItem == null) {
                throw new RuntimeException("关联的合同明细不存在");
            }
            int remaining = (contractItem.getQuantity() != null ? contractItem.getQuantity() : 0)
                    - (contractItem.getInStockQuantity() != null ? contractItem.getInStockQuantity() : 0);
            if (item.getQuantity() > remaining) {
                throw new RuntimeException("入库数量不能超过合同剩余可入库数量（剩余" + remaining + "）");
            }
        }
    }

    private void saveOrderItems(Long orderId, List<StockInItem> items) {
        List<StockInItem> validItems = items.stream()
                .filter(i -> i.getQuantity() != null && i.getQuantity() > 0)
                .collect(Collectors.toList());

        List<Long> contractItemIds = validItems.stream()
                .map(StockInItem::getContractItemId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, PurchaseContractItem> contractItemMap = new HashMap<>();
        if (!contractItemIds.isEmpty()) {
            List<PurchaseContractItem> contractItems = contractItemService.listByIds(contractItemIds);
            contractItemMap = contractItems.stream()
                    .collect(Collectors.toMap(PurchaseContractItem::getId, c -> c));
        }

        for (StockInItem item : validItems) {
            item.setOrderId(orderId);

            PurchaseContractItem contractItem = contractItemMap.get(item.getContractItemId());
            if (contractItem != null) {
                item.setProductId(contractItem.getProductId());
                item.setMaterialId(contractItem.getMaterialId());
                item.setSpecId(contractItem.getSpecId());
                item.setUnitPrice(contractItem.getUnitPrice());

                BigDecimal theoreticalWeight = computeTheoreticalWeight(contractItem, item.getQuantity());
                item.setTheoreticalWeight(theoreticalWeight);
            } else {
                if (item.getTheoreticalWeight() == null) {
                    item.setTheoreticalWeight(BigDecimal.ZERO);
                }
                if (item.getUnitPrice() == null) {
                    item.setUnitPrice(BigDecimal.ZERO);
                }
            }

            BigDecimal actualWeight = item.getActualWeight() != null ? item.getActualWeight() : BigDecimal.ZERO;
            item.setDeviationRate(computeDeviationRate(actualWeight, item.getTheoreticalWeight()));

            BigDecimal amount = (item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setAmount(amount);

            item.setCreateTime(now());
            stockInItemService.save(item);
        }
    }

    private BigDecimal computeTheoreticalWeight(PurchaseContractItem contractItem, Integer quantity) {
        if (contractItem == null || contractItem.getQuantity() == null || contractItem.getQuantity() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalWeight = contractItem.getWeight() != null ? contractItem.getWeight() : BigDecimal.ZERO;
        BigDecimal perUnit = totalWeight.divide(BigDecimal.valueOf(contractItem.getQuantity()), 6, RoundingMode.HALF_UP);
        return perUnit.multiply(BigDecimal.valueOf(quantity)).setScale(3, RoundingMode.HALF_UP);
    }

    private BigDecimal computeDeviationRate(BigDecimal actualWeight, BigDecimal theoreticalWeight) {
        if (theoreticalWeight == null || theoreticalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return actualWeight.subtract(theoreticalWeight)
                .divide(theoreticalWeight, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private void recalculateOrderTotals(Long orderId) {
        List<StockInItem> items = stockInItemService.listByOrderId(orderId);
        int totalQuantity = 0;
        BigDecimal totalTheoretical = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (StockInItem item : items) {
            if (item.getQuantity() != null) {
                totalQuantity += item.getQuantity();
            }
            if (item.getTheoreticalWeight() != null) {
                totalTheoretical = totalTheoretical.add(item.getTheoreticalWeight());
            }
            if (item.getActualWeight() != null) {
                totalActual = totalActual.add(item.getActualWeight());
            }
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
        }
        StockInOrder update = new StockInOrder();
        update.setId(orderId);
        update.setTotalQuantity(totalQuantity);
        update.setTotalTheoreticalWeight(totalTheoretical);
        update.setTotalActualWeight(totalActual);
        update.setTotalAmount(totalAmount);
        update.setUpdateTime(now());
        this.updateById(update);
    }

    @Override
    @Transactional
    public boolean audit(Long id, Long auditBy) {
        StockInOrder order = this.getById(id);
        if (order == null) {
            return false;
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只有待审核状态的入库单可以审核");
        }
        if (order.getWarehouseId() == null) {
            throw new RuntimeException("请选择入库仓库");
        }

        List<StockInItem> items = stockInItemService.listByOrderId(id);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("入库明细不能为空");
        }

        Map<Long, Integer> inboundMap = new HashMap<>();
        for (StockInItem item : items) {
            if (item.getFurnaceNo() == null || item.getFurnaceNo().trim().isEmpty()) {
                throw new RuntimeException("入库明细必须录入炉批号");
            }
            if (item.getActualWeight() == null || item.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("入库明细必须录入过磅重量");
            }

            stockService.increaseStock(
                    item.getProductId(),
                    item.getMaterialId(),
                    item.getSpecId(),
                    order.getWarehouseId(),
                    item.getFurnaceNo(),
                    item.getQuantity(),
                    item.getActualWeight(),
                    item.getUnitPrice()
            );

            if (item.getContractItemId() != null) {
                inboundMap.merge(item.getContractItemId(), item.getQuantity(), Integer::sum);
            }
        }

        for (Map.Entry<Long, Integer> entry : inboundMap.entrySet()) {
            Long contractItemId = entry.getKey();
            Integer qty = entry.getValue();
            PurchaseContractItem contractItem = contractItemService.getById(contractItemId);
            if (contractItem != null) {
                int contractQty = contractItem.getQuantity() != null ? contractItem.getQuantity() : 0;
                int alreadyIn = contractItem.getInStockQuantity() != null ? contractItem.getInStockQuantity() : 0;
                int newInStock = alreadyIn + qty;
                if (newInStock > contractQty) {
                    throw new RuntimeException("入库数量超过合同数量");
                }
                PurchaseContractItem update = new PurchaseContractItem();
                update.setId(contractItemId);
                update.setInStockQuantity(newInStock);
                contractItemService.updateById(update);

                inTransitStockService.releaseInTransitStock(contractItemId, qty);
            }
        }

        order.setStatus("AUDITED");
        order.setAuditBy(auditBy);
        order.setAuditTime(now());
        order.setUpdateTime(now());
        this.updateById(order);

        updateContractStatus(order.getContractId());

        return true;
    }

    private void updateContractStatus(Long contractId) {
        PurchaseContract contract = purchaseContractService.getById(contractId);
        if (contract == null || !"AUDITED".equals(contract.getStatus())) {
            return;
        }
        List<PurchaseContractItem> contractItems = contractItemService.listByContractId(contractId);
        boolean allCompleted = contractItems.stream().allMatch(ci -> {
            int qty = ci.getQuantity() != null ? ci.getQuantity() : 0;
            int inStock = ci.getInStockQuantity() != null ? ci.getInStockQuantity() : 0;
            return inStock >= qty;
        });
        if (allCompleted) {
            PurchaseContract update = new PurchaseContract();
            update.setId(contractId);
            update.setStatus("COMPLETED");
            update.setUpdateTime(now());
            purchaseContractService.updateById(update);
            inTransitStockService.deleteByContractId(contractId);
        }
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        StockInOrder order = this.getById(id);
        if (order == null) {
            return false;
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只有待审核状态的入库单可以删除");
        }
        stockInItemService.deleteByOrderId(id);
        return this.removeById(id);
    }
}
