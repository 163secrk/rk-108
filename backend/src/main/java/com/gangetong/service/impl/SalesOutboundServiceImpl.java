package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.SalesOutboundDTO;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.Partner;
import com.gangetong.entity.Product;
import com.gangetong.entity.SalesOrder;
import com.gangetong.entity.SalesOrderItem;
import com.gangetong.entity.SalesOutbound;
import com.gangetong.entity.SalesOutboundItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.Stock;
import com.gangetong.entity.User;
import com.gangetong.entity.Warehouse;
import com.gangetong.mapper.PartnerMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.SalesOrderItemMapper;
import com.gangetong.mapper.SalesOrderMapper;
import com.gangetong.mapper.SalesOutboundItemMapper;
import com.gangetong.mapper.SalesOutboundMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.mapper.StockMapper;
import com.gangetong.mapper.UserMapper;
import com.gangetong.mapper.WarehouseMapper;
import com.gangetong.service.InventoryLockService;
import com.gangetong.service.SalesOrderItemService;
import com.gangetong.service.SalesOrderService;
import com.gangetong.service.SalesOutboundItemService;
import com.gangetong.service.SalesOutboundService;
import com.gangetong.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SalesOutboundServiceImpl extends ServiceImpl<SalesOutboundMapper, SalesOutbound> implements SalesOutboundService {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderItemService orderItemService;

    @Autowired
    private SalesOrderItemMapper orderItemMapper;

    @Autowired
    private SalesOrderMapper salesOrderMapper;

    @Autowired
    private InventoryLockService inventoryLockService;

    @Autowired
    private StockService stockService;

    @Autowired
    private SalesOutboundItemService outboundItemService;

    @Autowired
    private SalesOutboundItemMapper outboundItemMapper;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private UserMapper userMapper;

    private static final Map<String, String> STATUS_TEXT_MAP = new HashMap<>();

    static {
        STATUS_TEXT_MAP.put("DRAFT", "待审核");
        STATUS_TEXT_MAP.put("AUDITED", "已审核");
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<SalesOutbound> listAll() {
        LambdaQueryWrapper<SalesOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SalesOutbound::getCreateTime);
        List<SalesOutbound> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<SalesOutbound> listByStatus(String status) {
        LambdaQueryWrapper<SalesOutbound> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SalesOutbound::getStatus, status);
        }
        wrapper.orderByDesc(SalesOutbound::getCreateTime);
        List<SalesOutbound> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<SalesOutbound> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> orderIds = list.stream()
                .map(SalesOutbound::getOrderId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> orderNoMap = new HashMap<>();
        if (!orderIds.isEmpty()) {
            List<SalesOrder> orders = salesOrderMapper.selectBatchIds(orderIds);
            orderNoMap = orders.stream()
                    .collect(Collectors.toMap(SalesOrder::getId, SalesOrder::getOrderNo));
        }

        List<Long> customerIds = list.stream()
                .map(SalesOutbound::getCustomerId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Partner> customerMap = new HashMap<>();
        if (!customerIds.isEmpty()) {
            List<Partner> customers = partnerMapper.selectBatchIds(customerIds);
            customerMap = customers.stream()
                    .collect(Collectors.toMap(Partner::getId, p -> p));
        }

        List<Long> userIds = list.stream()
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

        for (SalesOutbound outbound : list) {
            outbound.setOrderNo(orderNoMap.get(outbound.getOrderId()));
            Partner customer = customerMap.get(outbound.getCustomerId());
            if (customer != null) {
                outbound.setCustomerName(customer.getName());
                outbound.setCustomerCode(customer.getCode());
            }
            outbound.setStatusText(STATUS_TEXT_MAP.get(outbound.getStatus()));
            if (outbound.getCreateBy() != null) {
                outbound.setCreateByName(userNameMap.get(outbound.getCreateBy()));
            }
            if (outbound.getAuditBy() != null) {
                outbound.setAuditByName(userNameMap.get(outbound.getAuditBy()));
            }
        }
    }

    @Override
    public SalesOutbound getDetailById(Long id) {
        SalesOutbound outbound = this.getById(id);
        if (outbound != null) {
            List<SalesOutbound> wrapper = java.util.Collections.singletonList(outbound);
            fillRelatedData(wrapper);
            List<SalesOutboundItem> items = outboundItemService.listByOutboundId(id);
            fillItemsRelatedData(items);
            outbound.setItems(items);
        }
        return outbound;
    }

    private void fillItemsRelatedData(List<SalesOutboundItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> productIds = items.stream()
                .map(SalesOutboundItem::getProductId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Product> productMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<Product> products = productMapper.selectBatchIds(productIds);
            productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
        }
        List<Long> specIds = items.stream()
                .map(SalesOutboundItem::getSpecId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SteelSpec> specMap = new HashMap<>();
        if (!specIds.isEmpty()) {
            List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
            specMap = specs.stream()
                    .collect(Collectors.toMap(SteelSpec::getId, s -> s));
        }
        List<Long> warehouseIds = items.stream()
                .map(SalesOutboundItem::getWarehouseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!warehouseIds.isEmpty()) {
            List<Warehouse> warehouses = warehouseMapper.selectBatchIds(warehouseIds);
            warehouseNameMap = warehouses.stream()
                    .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        }

        for (SalesOutboundItem item : items) {
            Product p = productMap.get(item.getProductId());
            if (p != null) {
                item.setProductCode(p.getProductCode());
                item.setProductName(p.getProductName());
            }
            SteelSpec s = specMap.get(item.getSpecId());
            if (s != null) {
                item.setDiameter(s.getDiameter());
                item.setWallThickness(s.getWallThickness());
                item.setLength(s.getLength());
                item.setWeightPerMeter(s.getWeightPerMeter());
            }
            item.setWarehouseName(warehouseNameMap.get(item.getWarehouseId()));
        }
    }

    @Override
    public String generateOutboundNo() {
        String prefix = "CK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<SalesOutbound> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SalesOutbound::getOutboundNo, prefix + "%")
                .orderByDesc(SalesOutbound::getOutboundNo)
                .last("limit 1");
        SalesOutbound last = this.getOne(wrapper);

        int sequence = 1;
        if (last != null && last.getOutboundNo() != null) {
            try {
                String seqStr = last.getOutboundNo().substring(prefix.length());
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    @Override
    public SalesOutbound generateFromOrder(Long orderId) {
        SalesOrder order = salesOrderService.getById(orderId);
        if (order == null) {
            throw new RuntimeException("销售订单不存在");
        }
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只能对已确认状态的销售订单进行出库");
        }

        SalesOutbound outbound = new SalesOutbound();
        outbound.setOrderId(orderId);
        outbound.setCustomerId(order.getCustomerId());
        outbound.setStatus("DRAFT");

        List<SalesOrderItem> orderItems = orderItemService.listByOrderId(orderId);
        List<SalesOutboundItem> outboundItems = new ArrayList<>();
        int sortNo = 1;
        int totalQty = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (SalesOrderItem orderItem : orderItems) {
            List<InventoryLock> locks = inventoryLockService.listByOrderItemId(orderItem.getId());
            for (InventoryLock lock : locks) {
                SalesOutboundItem item = new SalesOutboundItem();
                item.setOrderItemId(orderItem.getId());
                item.setLockId(lock.getId());
                item.setProductId(orderItem.getProductId());
                item.setMaterialId(orderItem.getMaterialId());
                item.setSpecId(orderItem.getSpecId());
                item.setStockId(lock.getStockId());
                item.setWarehouseId(lock.getWarehouseId());
                item.setFurnaceNo(lock.getFurnaceNo());
                item.setPlanQuantity(lock.getLockQuantity());
                item.setActualQuantity(lock.getLockQuantity());
                item.setPlanWeight(lock.getLockWeight());
                item.setActualWeight(lock.getLockWeight());
                item.setUnitPrice(orderItem.getUnitPrice());
                BigDecimal amount = orderItem.getUnitPrice() != null
                        ? orderItem.getUnitPrice().multiply(BigDecimal.valueOf(lock.getLockQuantity()))
                        : BigDecimal.ZERO;
                item.setAmount(amount);
                item.setSortNo(sortNo++);
                outboundItems.add(item);

                totalQty += lock.getLockQuantity();
                totalWeight = totalWeight.add(lock.getLockWeight() != null ? lock.getLockWeight() : BigDecimal.ZERO);
            }
        }

        outbound.setTotalQuantity(totalQty);
        outbound.setTotalWeight(totalWeight);
        outbound.setItems(outboundItems);

        return outbound;
    }

    @Override
    @Transactional
    public boolean add(SalesOutboundDTO dto) {
        if (dto.getOrderId() == null) {
            throw new RuntimeException("请选择销售订单");
        }
        SalesOrder order = salesOrderService.getById(dto.getOrderId());
        if (order == null) {
            throw new RuntimeException("销售订单不存在");
        }
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new RuntimeException("只能对已确认状态的销售订单进行出库");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("出库明细不能为空");
        }

        validateItems(dto.getItems());

        SalesOutbound outbound = new SalesOutbound();
        outbound.setOutboundNo(dto.getOutboundNo() != null ? dto.getOutboundNo() : generateOutboundNo());
        outbound.setOrderId(dto.getOrderId());
        outbound.setCustomerId(order.getCustomerId());
        outbound.setStatus("DRAFT");
        outbound.setPlateNo(dto.getPlateNo());
        outbound.setDriverName(dto.getDriverName());
        outbound.setDriverPhone(dto.getDriverPhone());
        outbound.setRemark(dto.getRemark());
        outbound.setCreateBy(dto.getCreateBy());
        outbound.setCreateTime(now());
        outbound.setUpdateTime(now());

        boolean saved = this.save(outbound);
        if (!saved) {
            return false;
        }

        saveOutboundItems(outbound.getId(), dto.getItems());
        recalculateTotals(outbound.getId());
        return true;
    }

    private void validateItems(List<SalesOutboundItem> items) {
        List<SalesOutboundItem> validItems = items.stream()
                .filter(i -> i.getActualQuantity() != null && i.getActualQuantity() > 0)
                .collect(Collectors.toList());
        if (validItems.isEmpty()) {
            throw new RuntimeException("请至少录入一条有效的出库明细");
        }

        for (SalesOutboundItem item : validItems) {
            if (item.getLockId() == null) {
                throw new RuntimeException("出库明细必须关联库存锁定记录");
            }
            InventoryLock lock = inventoryLockService.getById(item.getLockId());
            if (lock == null) {
                throw new RuntimeException("关联的库存锁定记录不存在");
            }
            if (!"LOCKED".equals(lock.getStatus())) {
                throw new RuntimeException("库存锁定记录状态无效，已被释放或使用");
            }
            if (item.getActualQuantity() > lock.getLockQuantity()) {
                throw new RuntimeException("实发数量不能超过锁定数量（锁定" + lock.getLockQuantity() + "）");
            }
            if (item.getActualWeight() == null || item.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("请录入实发重量");
            }
        }
    }

    private void saveOutboundItems(Long outboundId, List<SalesOutboundItem> items) {
        List<SalesOutboundItem> validItems = items.stream()
                .filter(i -> i.getActualQuantity() != null && i.getActualQuantity() > 0)
                .collect(Collectors.toList());

        for (int i = 0; i < validItems.size(); i++) {
            SalesOutboundItem item = validItems.get(i);
            item.setOutboundId(outboundId);
            item.setSortNo(i + 1);

            if (item.getUnitPrice() == null) {
                item.setUnitPrice(BigDecimal.ZERO);
            }
            BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getActualQuantity()));
            item.setAmount(amount);

            if (item.getActualWeight() == null) {
                item.setActualWeight(BigDecimal.ZERO);
            }

            item.setCreateTime(now());
            outboundItemService.save(item);
        }
    }

    private void recalculateTotals(Long outboundId) {
        List<SalesOutboundItem> items = outboundItemService.listByOutboundId(outboundId);
        int totalQty = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (SalesOutboundItem item : items) {
            if (item.getActualQuantity() != null) {
                totalQty += item.getActualQuantity();
            }
            if (item.getActualWeight() != null) {
                totalWeight = totalWeight.add(item.getActualWeight());
            }
        }
        SalesOutbound update = new SalesOutbound();
        update.setId(outboundId);
        update.setTotalQuantity(totalQty);
        update.setTotalWeight(totalWeight);
        update.setUpdateTime(now());
        this.updateById(update);
    }

    @Override
    @Transactional
    public boolean update(SalesOutboundDTO dto) {
        SalesOutbound outbound = this.getById(dto.getId());
        if (outbound == null) {
            return false;
        }
        if (!"DRAFT".equals(outbound.getStatus())) {
            throw new RuntimeException("只有待审核状态的出库单可以修改");
        }

        validateItems(dto.getItems());

        outbound.setPlateNo(dto.getPlateNo());
        outbound.setDriverName(dto.getDriverName());
        outbound.setDriverPhone(dto.getDriverPhone());
        outbound.setRemark(dto.getRemark());
        outbound.setUpdateTime(now());

        boolean updated = this.updateById(outbound);
        if (!updated) {
            return false;
        }

        if (dto.getDeletedItemIds() != null && !dto.getDeletedItemIds().isEmpty()) {
            outboundItemService.removeByIds(dto.getDeletedItemIds());
        }

        saveOutboundItems(outbound.getId(), dto.getItems());
        recalculateTotals(outbound.getId());
        return true;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        SalesOutbound outbound = this.getById(id);
        if (outbound == null) {
            return false;
        }
        if (!"DRAFT".equals(outbound.getStatus())) {
            throw new RuntimeException("只有待审核状态的出库单可以删除");
        }
        outboundItemService.deleteByOutboundId(id);
        return this.removeById(id);
    }

    @Override
    @Transactional
    public boolean audit(Long id, Long auditBy) {
        SalesOutbound outbound = this.getById(id);
        if (outbound == null) {
            return false;
        }
        if (!"DRAFT".equals(outbound.getStatus())) {
            throw new RuntimeException("只有待审核状态的出库单可以审核");
        }

        List<SalesOutboundItem> items = outboundItemService.listByOutboundId(id);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("出库明细不能为空");
        }

        Map<Long, Integer> orderItemOutboundMap = new HashMap<>();

        for (SalesOutboundItem item : items) {
            if (item.getLockId() == null) {
                throw new RuntimeException("出库明细必须关联库存锁定记录");
            }

            InventoryLock lock = inventoryLockService.getById(item.getLockId());
            if (lock == null) {
                throw new RuntimeException("关联的库存锁定记录不存在");
            }
            if (!"LOCKED".equals(lock.getStatus())) {
                throw new RuntimeException("库存锁定记录状态无效，已被释放或使用");
            }

            int actualQty = item.getActualQuantity() != null ? item.getActualQuantity() : 0;
            BigDecimal actualWeight = item.getActualWeight() != null ? item.getActualWeight() : BigDecimal.ZERO;

            if (actualQty <= 0) {
                continue;
            }

            stockService.decreaseStock(
                    lock.getStockId(),
                    actualQty,
                    actualWeight
            );

            lock.setStatus("RELEASED");
            lock.setReleaseTime(now());
            inventoryLockService.updateById(lock);

            orderItemOutboundMap.merge(item.getOrderItemId(), actualQty, Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : orderItemOutboundMap.entrySet()) {
            Long orderItemId = entry.getKey();
            Integer qty = entry.getValue();
            SalesOrderItem orderItem = orderItemService.getById(orderItemId);
            if (orderItem != null) {
                int alreadyOut = orderItem.getOutStockQuantity() != null ? orderItem.getOutStockQuantity() : 0;
                int newOutQty = alreadyOut + qty;
                SalesOrderItem update = new SalesOrderItem();
                update.setId(orderItemId);
                update.setOutStockQuantity(newOutQty);
                orderItemService.updateById(update);
            }
        }

        updateOrderStatus(outbound.getOrderId());

        outbound.setStatus("AUDITED");
        outbound.setAuditBy(auditBy);
        outbound.setAuditTime(now());
        outbound.setUpdateTime(now());
        this.updateById(outbound);

        return true;
    }

    private void updateOrderStatus(Long orderId) {
        SalesOrder order = salesOrderService.getById(orderId);
        if (order == null) {
            return;
        }
        List<SalesOrderItem> orderItems = orderItemService.listByOrderId(orderId);
        boolean allCompleted = orderItems.stream().allMatch(oi -> {
            int qty = oi.getQuantity() != null ? oi.getQuantity() : 0;
            int outQty = oi.getOutStockQuantity() != null ? oi.getOutStockQuantity() : 0;
            return outQty >= qty;
        });
        if (allCompleted) {
            SalesOrder update = new SalesOrder();
            update.setId(orderId);
            update.setStatus("COMPLETED");
            update.setUpdateTime(now());
            salesOrderService.updateById(update);
        }
    }
}
