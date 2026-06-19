package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.SalesOrderDTO;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.Partner;
import com.gangetong.entity.Product;
import com.gangetong.entity.SalesOrder;
import com.gangetong.entity.SalesOrderItem;
import com.gangetong.entity.SteelSpec;
import com.gangetong.entity.Stock;
import com.gangetong.entity.User;
import com.gangetong.mapper.PartnerMapper;
import com.gangetong.mapper.ProductMapper;
import com.gangetong.mapper.SalesOrderItemMapper;
import com.gangetong.mapper.SalesOrderMapper;
import com.gangetong.mapper.SteelSpecMapper;
import com.gangetong.mapper.StockMapper;
import com.gangetong.mapper.UserMapper;
import com.gangetong.service.InventoryLockService;
import com.gangetong.service.SalesOrderItemService;
import com.gangetong.service.SalesOrderService;
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
public class SalesOrderServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrder> implements SalesOrderService {

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SteelSpecMapper steelSpecMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private SalesOrderItemService orderItemService;

    @Autowired
    private SalesOrderItemMapper orderItemMapper;

    @Autowired
    private InventoryLockService inventoryLockService;

    private static final Map<String, String> STATUS_TEXT_MAP = new HashMap<>();

    static {
        STATUS_TEXT_MAP.put("DRAFT", "草稿");
        STATUS_TEXT_MAP.put("CONFIRMED", "已确认");
        STATUS_TEXT_MAP.put("COMPLETED", "已完成");
        STATUS_TEXT_MAP.put("CANCELLED", "已取消");
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<SalesOrder> listAll() {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SalesOrder::getCreateTime);
        List<SalesOrder> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<SalesOrder> listByStatus(String status) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SalesOrder::getStatus, status);
        }
        wrapper.orderByDesc(SalesOrder::getCreateTime);
        List<SalesOrder> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<SalesOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        List<Long> customerIds = orders.stream()
                .map(SalesOrder::getCustomerId)
                .distinct()
                .collect(Collectors.toList());
        List<Partner> customers = partnerMapper.selectBatchIds(customerIds);
        Map<Long, Partner> customerMap = customers.stream()
                .collect(Collectors.toMap(Partner::getId, p -> p));

        List<Long> createByIds = orders.stream()
                .map(SalesOrder::getCreateBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!createByIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(createByIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname));
        }

        for (SalesOrder order : orders) {
            Partner customer = customerMap.get(order.getCustomerId());
            if (customer != null) {
                order.setCustomerName(customer.getName());
                order.setCustomerCode(customer.getCode());
            }
            order.setStatusText(STATUS_TEXT_MAP.get(order.getStatus()));
            if (order.getCreateBy() != null) {
                order.setCreateByName(userNameMap.get(order.getCreateBy()));
            }
        }
    }

    @Override
    public SalesOrder getDetailById(Long id) {
        SalesOrder order = this.getById(id);
        if (order != null) {
            Partner customer = partnerMapper.selectById(order.getCustomerId());
            if (customer != null) {
                order.setCustomerName(customer.getName());
                order.setCustomerCode(customer.getCode());
                order.setCreditLimit(customer.getCreditLimit());
                order.setCurrentDebt(calculateCustomerDebt(order.getCustomerId()));
            }
            order.setStatusText(STATUS_TEXT_MAP.get(order.getStatus()));
            if (order.getCreateBy() != null) {
                User user = userMapper.selectById(order.getCreateBy());
                if (user != null) {
                    order.setCreateByName(user.getNickname());
                }
            }
            List<SalesOrderItem> items = orderItemService.listByOrderId(id);
            fillItemsRelatedData(items);
            for (SalesOrderItem item : items) {
                List<InventoryLock> locks = inventoryLockService.listByOrderItemId(item.getId());
                fillLocksRelatedData(locks);
                item.setStockLocks(locks);
            }
            order.setItems(items);
        }
        return order;
    }

    private void fillItemsRelatedData(List<SalesOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> productIds = items.stream()
                .map(SalesOrderItem::getProductId)
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
                .map(SalesOrderItem::getSpecId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SteelSpec> specMap = new HashMap<>();
        if (!specIds.isEmpty()) {
            List<SteelSpec> specs = steelSpecMapper.selectBatchIds(specIds);
            specMap = specs.stream()
                    .collect(Collectors.toMap(SteelSpec::getId, s -> s));
        }
        for (SalesOrderItem item : items) {
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
            int remaining = (item.getQuantity() != null ? item.getQuantity() : 0)
                    - (item.getOutStockQuantity() != null ? item.getOutStockQuantity() : 0);
            item.setRemainingQuantity(remaining);
        }
    }

    private void fillLocksRelatedData(List<InventoryLock> locks) {
        // 可以在这里填充仓库名称等，暂时省略
    }

    @Override
    public String generateOrderNo() {
        String prefix = "XS" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(SalesOrder::getOrderNo, prefix + "%");
        wrapper.orderByDesc(SalesOrder::getOrderNo);
        wrapper.last("limit 1");
        SalesOrder last = this.getOne(wrapper);

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
    public boolean add(SalesOrderDTO dto) {
        SalesOrder order = new SalesOrder();
        order.setOrderNo(dto.getOrderNo() != null ? dto.getOrderNo() : generateOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setOrderDate(dto.getOrderDate());
        order.setStatus("DRAFT");
        order.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        order.setTotalWeight(dto.getTotalWeight() != null ? dto.getTotalWeight() : BigDecimal.ZERO);
        order.setIsOverCredit(0);
        order.setRemark(dto.getRemark());
        order.setCreateBy(dto.getCreateBy());
        order.setCreateTime(now());
        order.setUpdateTime(now());

        boolean saved = this.save(order);
        if (!saved) {
            return false;
        }

        saveOrderItems(order.getId(), dto.getItems());
        recalculateOrderAmount(order.getId());

        return true;
    }

    @Override
    @Transactional
    public boolean update(SalesOrderDTO dto) {
        SalesOrder order = this.getById(dto.getId());
        if (order == null) {
            return false;
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只有草稿状态的订单可以修改");
        }

        order.setCustomerId(dto.getCustomerId());
        order.setOrderDate(dto.getOrderDate());
        order.setTotalAmount(dto.getTotalAmount() != null ? dto.getTotalAmount() : BigDecimal.ZERO);
        order.setTotalWeight(dto.getTotalWeight() != null ? dto.getTotalWeight() : BigDecimal.ZERO);
        order.setRemark(dto.getRemark());
        order.setUpdateTime(now());

        boolean updated = this.updateById(order);
        if (!updated) {
            return false;
        }

        if (dto.getDeletedItemIds() != null && !dto.getDeletedItemIds().isEmpty()) {
            orderItemService.removeByIds(dto.getDeletedItemIds());
        }

        saveOrderItems(order.getId(), dto.getItems());
        recalculateOrderAmount(order.getId());

        return true;
    }

    private void saveOrderItems(Long orderId, List<SalesOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            SalesOrderItem item = items.get(i);
            item.setOrderId(orderId);
            item.setSortNo(i + 1);

            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                item.setMaterialId(product.getMaterialId());
                item.setSpecId(product.getSpecId());
            }

            if (item.getQuantity() == null) {
                item.setQuantity(0);
            }
            if (item.getUnitPrice() == null) {
                item.setUnitPrice(BigDecimal.ZERO);
            }

            BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setAmount(amount);

            if (item.getSpecId() != null && item.getQuantity() != null) {
                SteelSpec spec = steelSpecMapper.selectById(item.getSpecId());
                if (spec != null && spec.getWeightPerMeter() != null && spec.getLength() != null) {
                    try {
                        BigDecimal lengthM = new BigDecimal(spec.getLength()).divide(BigDecimal.valueOf(1000), 4, BigDecimal.ROUND_HALF_UP);
                        BigDecimal weight = spec.getWeightPerMeter()
                                .multiply(lengthM)
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                        item.setWeight(weight);
                    } catch (Exception e) {
                        item.setWeight(BigDecimal.ZERO);
                    }
                } else {
                    item.setWeight(BigDecimal.ZERO);
                }
            } else {
                item.setWeight(BigDecimal.ZERO);
            }

            if (item.getId() == null) {
                item.setCreateTime(now());
                orderItemService.save(item);
            } else {
                orderItemService.updateById(item);
            }
        }
    }

    private void recalculateOrderAmount(Long orderId) {
        List<SalesOrderItem> items = orderItemService.listByOrderId(orderId);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (SalesOrderItem item : items) {
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
            if (item.getWeight() != null) {
                totalWeight = totalWeight.add(item.getWeight());
            }
        }

        SalesOrder order = new SalesOrder();
        order.setId(orderId);
        order.setTotalAmount(totalAmount);
        order.setTotalWeight(totalWeight);
        this.updateById(order);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        SalesOrder order = this.getById(id);
        if (order == null) {
            return false;
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只有草稿状态的订单可以删除");
        }

        inventoryLockService.releaseByOrderId(id);
        orderItemService.deleteByOrderId(id);
        return this.removeById(id);
    }

    @Override
    @Transactional
    public Map<String, Object> confirm(Long id) {
        Map<String, Object> result = new HashMap<>();
        SalesOrder order = this.getById(id);
        if (order == null) {
            result.put("success", false);
            result.put("message", "订单不存在");
            return result;
        }
        if (!"DRAFT".equals(order.getStatus())) {
            result.put("success", false);
            result.put("message", "只有草稿状态的订单可以确认");
            return result;
        }

        List<SalesOrderItem> items = orderItemService.listByOrderId(id);
        if (items == null || items.isEmpty()) {
            result.put("success", false);
            result.put("message", "订单明细不能为空");
            return result;
        }

        // 信用额度检查
        Map<String, Object> creditCheck = checkCredit(order.getCustomerId(), order.getTotalAmount());
        boolean isOverCredit = (Boolean) creditCheck.get("isOverCredit");
        order.setIsOverCredit(isOverCredit ? 1 : 0);

        // 校验库存锁定并执行
        for (SalesOrderItem item : items) {
            // 这里简化：前端已经通过库存选择器选定了批次，DTO中的locks里包含了批次信息
            // 但在确认时，我们再验证一次可用库存
            int totalNeeded = item.getQuantity() != null ? item.getQuantity() : 0;
            if (totalNeeded <= 0) {
                continue;
            }
            // 通过库存服务查询该商品规格的可用库存（排除当前订单）
            int totalAvailable = calculateAvailableForProduct(item.getProductId(), id);
            if (totalAvailable < totalNeeded) {
                result.put("success", false);
                result.put("message", "商品「" + item.getProductName() + "」可用库存不足，需要" + totalNeeded + "支，当前可用" + totalAvailable + "支");
                return result;
            }
        }

        // 执行库存锁定（按先进先出，从可用库存中锁定）
        inventoryLockService.releaseByOrderId(id);
        for (SalesOrderItem item : items) {
            int needed = item.getQuantity() != null ? item.getQuantity() : 0;
            if (needed <= 0) continue;
            lockStockForItem(id, item.getId(), item.getProductId(), needed);
        }

        order.setStatus("CONFIRMED");
        order.setUpdateTime(now());
        this.updateById(order);

        result.put("success", true);
        result.put("message", "确认成功");
        result.put("isOverCredit", isOverCredit);
        result.put("creditInfo", creditCheck);
        return result;
    }

    private int calculateAvailableForProduct(Long productId, Long excludeOrderId) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProductId, productId);
        List<Stock> stocks = stockMapper.selectList(wrapper);
        int total = 0;
        for (Stock s : stocks) {
            if (s.getQuantity() == null) continue;
            int locked = inventoryLockService.getAvailableQuantityByBatch(
                    s.getProductId(), s.getWarehouseId(), s.getFurnaceNo(), excludeOrderId);
            // getAvailableQuantityByBatch 已经是减去锁定后的可用量
            total += locked;
        }
        return total;
    }

    private void lockStockForItem(Long orderId, Long orderItemId, Long productId, int needed) {
        LambdaQueryWrapper<Stock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Stock::getProductId, productId)
                .orderByAsc(Stock::getCreateTime);
        List<Stock> stocks = stockMapper.selectList(wrapper);
        int remaining = needed;

        for (Stock s : stocks) {
            if (remaining <= 0) break;
            if (s.getQuantity() == null || s.getQuantity() <= 0) continue;

            int available = inventoryLockService.getAvailableQuantityByBatch(
                    s.getProductId(), s.getWarehouseId(), s.getFurnaceNo(), orderId);
            if (available <= 0) continue;

            int toLock = Math.min(available, remaining);
            Stock batchStock = new Stock();
            batchStock.setId(s.getId());
            batchStock.setProductId(s.getProductId());
            batchStock.setWarehouseId(s.getWarehouseId());
            batchStock.setFurnaceNo(s.getFurnaceNo());
            batchStock.setQuantity(toLock);
            // 计算锁定重量：按比例
            if (s.getWeight() != null && s.getQuantity() != null && s.getQuantity() > 0) {
                BigDecimal perQtyWeight = s.getWeight().divide(BigDecimal.valueOf(s.getQuantity()), 6, BigDecimal.ROUND_HALF_UP);
                batchStock.setWeight(perQtyWeight.multiply(BigDecimal.valueOf(toLock)));
            } else {
                batchStock.setWeight(BigDecimal.ZERO);
            }

            List<Stock> batchList = new ArrayList<>();
            batchList.add(batchStock);
            inventoryLockService.lockStocks(orderId, orderItemId, batchList);
            remaining -= toLock;
        }
    }

    @Override
    public Map<String, Object> checkCredit(Long customerId, BigDecimal orderAmount) {
        Map<String, Object> result = new HashMap<>();
        Partner customer = partnerMapper.selectById(customerId);
        if (customer == null) {
            result.put("isOverCredit", false);
            result.put("creditLimit", BigDecimal.ZERO);
            result.put("currentDebt", BigDecimal.ZERO);
            result.put("orderAmount", orderAmount != null ? orderAmount : BigDecimal.ZERO);
            result.put("totalAfter", orderAmount != null ? orderAmount : BigDecimal.ZERO);
            result.put("message", "客户不存在");
            return result;
        }

        BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal currentDebt = calculateCustomerDebt(customerId);
        BigDecimal orderAmt = orderAmount != null ? orderAmount : BigDecimal.ZERO;
        BigDecimal totalAfter = currentDebt.add(orderAmt);

        boolean isOverCredit = creditLimit.compareTo(BigDecimal.ZERO) > 0 && totalAfter.compareTo(creditLimit) > 0;

        result.put("isOverCredit", isOverCredit);
        result.put("creditLimit", creditLimit);
        result.put("currentDebt", currentDebt);
        result.put("orderAmount", orderAmt);
        result.put("totalAfter", totalAfter);
        if (isOverCredit) {
            BigDecimal exceed = totalAfter.subtract(creditLimit);
            result.put("message", "客户「" + customer.getName() + "」信用额度超额！信用额度：" + creditLimit
                    + "元，当前欠款：" + currentDebt + "元，本订单金额：" + orderAmt
                    + "元，合计：" + totalAfter + "元，超额：" + exceed + "元");
        } else {
            result.put("message", "信用额度正常");
        }
        return result;
    }

    @Override
    public BigDecimal calculateCustomerDebt(Long customerId) {
        // 简化实现：统计所有已确认但未完成的销售订单金额之和作为欠款
        // 实际项目中应根据应收账款表计算
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SalesOrder::getCustomerId, customerId)
                .in(SalesOrder::getStatus, "CONFIRMED", "COMPLETED");
        List<SalesOrder> orders = this.list(wrapper);
        BigDecimal debt = BigDecimal.ZERO;
        for (SalesOrder o : orders) {
            if (o.getTotalAmount() != null) {
                debt = debt.add(o.getTotalAmount());
            }
        }
        return debt;
    }
}
