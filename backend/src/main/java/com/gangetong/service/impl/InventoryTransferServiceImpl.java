package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.dto.InventoryTransferDTO;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.InventoryTransfer;
import com.gangetong.entity.InventoryTransferItem;
import com.gangetong.entity.Stock;
import com.gangetong.entity.User;
import com.gangetong.entity.Warehouse;
import com.gangetong.mapper.InventoryTransferMapper;
import com.gangetong.mapper.UserMapper;
import com.gangetong.mapper.WarehouseMapper;
import com.gangetong.service.InTransitStockService;
import com.gangetong.service.InventoryTransferItemService;
import com.gangetong.service.InventoryTransferService;
import com.gangetong.service.StockService;
import com.gangetong.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryTransferServiceImpl extends ServiceImpl<InventoryTransferMapper, InventoryTransfer> implements InventoryTransferService {

    @Autowired
    private InventoryTransferItemService transferItemService;

    @Autowired
    private StockService stockService;

    @Autowired
    private InTransitStockService inTransitStockService;

    @Autowired
    private WarehouseMapper warehouseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WarehouseService warehouseService;

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public List<InventoryTransfer> listAll() {
        List<InventoryTransfer> list = this.list(new LambdaQueryWrapper<InventoryTransfer>().orderByDesc(InventoryTransfer::getCreateTime));
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<InventoryTransfer> listByStatus(String status) {
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(InventoryTransfer::getStatus, status);
        }
        wrapper.orderByDesc(InventoryTransfer::getCreateTime);
        List<InventoryTransfer> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<InventoryTransfer> listByFromWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(InventoryTransfer::getFromWarehouseId, warehouseId);
        }
        wrapper.orderByDesc(InventoryTransfer::getCreateTime);
        List<InventoryTransfer> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    @Override
    public List<InventoryTransfer> listByToWarehouseId(Long warehouseId) {
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<>();
        if (warehouseId != null) {
            wrapper.eq(InventoryTransfer::getToWarehouseId, warehouseId);
        }
        wrapper.orderByDesc(InventoryTransfer::getCreateTime);
        List<InventoryTransfer> list = this.list(wrapper);
        fillRelatedData(list);
        return list;
    }

    private void fillRelatedData(List<InventoryTransfer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Long> fromWarehouseIds = list.stream()
                .map(InventoryTransfer::getFromWarehouseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> toWarehouseIds = list.stream()
                .map(InventoryTransfer::getToWarehouseId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> allWarehouseIds = java.util.stream.Stream.concat(fromWarehouseIds.stream(), toWarehouseIds.stream())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> warehouseNameMap = new HashMap<>();
        if (!allWarehouseIds.isEmpty()) {
            List<Warehouse> warehouses = warehouseMapper.selectBatchIds(allWarehouseIds);
            warehouseNameMap = warehouses.stream()
                    .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        }

        List<Long> userIds = list.stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getCreateBy(), t.getAuditBy(), t.getReceiveBy()))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getNickname));
        }

        for (InventoryTransfer transfer : list) {
            transfer.setFromWarehouseName(warehouseNameMap.get(transfer.getFromWarehouseId()));
            transfer.setToWarehouseName(warehouseNameMap.get(transfer.getToWarehouseId()));
            transfer.setCreateByName(userNameMap.get(transfer.getCreateBy()));
            transfer.setAuditByName(userNameMap.get(transfer.getAuditBy()));
            transfer.setReceiveByName(userNameMap.get(transfer.getReceiveBy()));
            transfer.setStatusText(getStatusText(transfer.getStatus()));
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "DRAFT":
                return "草稿";
            case "AUDITED":
                return "已审核(在途)";
            case "RECEIVED":
                return "已收货";
            case "CANCELLED":
                return "已取消";
            default:
                return status;
        }
    }

    @Override
    public InventoryTransfer getDetailById(Long id) {
        InventoryTransfer transfer = this.getById(id);
        if (transfer == null) {
            return null;
        }
        fillRelatedData(java.util.Collections.singletonList(transfer));
        List<InventoryTransferItem> items = transferItemService.listByTransferIdWithDetail(id);
        transfer.setItems(items);
        return transfer;
    }

    @Override
    public String generateTransferNo() {
        String prefix = "DB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(InventoryTransfer::getTransferNo, prefix + "%");
        wrapper.orderByDesc(InventoryTransfer::getTransferNo);
        wrapper.last("limit 1");
        InventoryTransfer last = this.getOne(wrapper);
        int seq = 1;
        if (last != null && last.getTransferNo() != null) {
            String no = last.getTransferNo();
            String seqStr = no.substring(prefix.length());
            try {
                seq = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    @Override
    @Transactional
    public boolean add(InventoryTransferDTO dto) {
        if (dto.getFromWarehouseId() == null) {
            throw new RuntimeException("请选择调出仓库");
        }
        if (dto.getToWarehouseId() == null) {
            throw new RuntimeException("请选择目标仓库");
        }
        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId())) {
            throw new RuntimeException("调出仓库和目标仓库不能相同");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("请添加调拨明细");
        }

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.setTransferNo(dto.getTransferNo() != null ? dto.getTransferNo() : generateTransferNo());
        transfer.setFromWarehouseId(dto.getFromWarehouseId());
        transfer.setToWarehouseId(dto.getToWarehouseId());
        transfer.setStatus("DRAFT");
        transfer.setRemark(dto.getRemark());
        transfer.setCreateBy(dto.getCreateBy());
        transfer.setCreateTime(now());
        transfer.setUpdateTime(now());

        List<Long> fromWarehouseIds = warehouseService.listAllChildIds(dto.getFromWarehouseId());

        int totalQty = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;
        List<InventoryTransferItem> items = dto.getItems();
        for (int i = 0; i < items.size(); i++) {
            InventoryTransferItem item = items.get(i);
            if (item.getStockId() == null) {
                throw new RuntimeException("请选择库存批次");
            }
            Stock stock = stockService.getById(item.getStockId());
            if (stock == null) {
                throw new RuntimeException("库存记录不存在");
            }
            if (!fromWarehouseIds.contains(stock.getWarehouseId())) {
                throw new RuntimeException("库存不在调出仓库中不存在该库存");
            }
            int planQty = item.getPlanQuantity() != null ? item.getPlanQuantity() : 0;
            if (planQty <= 0) {
                throw new RuntimeException("调拨数量必须大于0");
            }
            if (planQty > (stock.getQuantity() != null ? stock.getQuantity() : 0)) {
                throw new RuntimeException("库存不足，当前库存：" + stock.getQuantity() + "，调拨数量：" + planQty);
            }

            item.setProductId(stock.getProductId());
            item.setMaterialId(stock.getMaterialId());
            item.setSpecId(stock.getSpecId());
            item.setFurnaceNo(stock.getFurnaceNo());
            item.setPlanWeight(stock.getWeight() != null && stock.getQuantity() != null && stock.getQuantity() > 0
                    ? stock.getWeight().divide(BigDecimal.valueOf(stock.getQuantity()), 6, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(planQty))
                    : BigDecimal.ZERO);
            item.setCostUnitPrice(stock.getCostUnitPrice());
            item.setCostAmount(stock.getCostUnitPrice() != null
                    ? stock.getCostUnitPrice().multiply(BigDecimal.valueOf(planQty))
                    : BigDecimal.ZERO);
            item.setSortNo(i + 1);

            totalQty += planQty;
            totalWeight = totalWeight.add(item.getPlanWeight() != null ? item.getPlanWeight() : BigDecimal.ZERO);
        }

        transfer.setTotalQuantity(totalQty);
        transfer.setTotalWeight(totalWeight);
        transfer.setReceivedQuantity(0);
        transfer.setReceivedWeight(BigDecimal.ZERO);

        boolean saved = this.save(transfer);
        if (!saved) {
            return false;
        }

        for (InventoryTransferItem item : items) {
            item.setTransferId(transfer.getId());
            item.setCreateTime(now());
        }
        transferItemService.saveBatch(items);

        return true;
    }

    @Override
    @Transactional
    public boolean update(InventoryTransferDTO dto) {
        InventoryTransfer transfer = this.getById(dto.getId());
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new RuntimeException("只有草稿状态的调拨单可以修改");
        }

        if (dto.getFromWarehouseId() == null) {
            throw new RuntimeException("请选择调出仓库");
        }
        if (dto.getToWarehouseId() == null) {
            throw new RuntimeException("请选择目标仓库");
        }
        if (dto.getFromWarehouseId().equals(dto.getToWarehouseId())) {
            throw new RuntimeException("调出仓库和目标仓库不能相同");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("请添加调拨明细");
        }

        transfer.setFromWarehouseId(dto.getFromWarehouseId());
        transfer.setToWarehouseId(dto.getToWarehouseId());
        transfer.setRemark(dto.getRemark());
        transfer.setUpdateTime(now());

        List<Long> fromWarehouseIds = warehouseService.listAllChildIds(dto.getFromWarehouseId());

        int totalQty = 0;
        BigDecimal totalWeight = BigDecimal.ZERO;
        List<InventoryTransferItem> items = dto.getItems();
        for (int i = 0; i < items.size(); i++) {
            InventoryTransferItem item = items.get(i);
            if (item.getStockId() == null) {
                throw new RuntimeException("请选择库存批次");
            }
            Stock stock = stockService.getById(item.getStockId());
            if (stock == null) {
                throw new RuntimeException("库存记录不存在");
            }
            if (!fromWarehouseIds.contains(stock.getWarehouseId())) {
                throw new RuntimeException("调出仓库中不存在该库存");
            }
            int planQty = item.getPlanQuantity() != null ? item.getPlanQuantity() : 0;
            if (planQty <= 0) {
                throw new RuntimeException("调拨数量必须大于0");
            }
            if (planQty > (stock.getQuantity() != null ? stock.getQuantity() : 0)) {
                throw new RuntimeException("库存不足，当前库存：" + stock.getQuantity() + "，调拨数量：" + planQty);
            }

            item.setProductId(stock.getProductId());
            item.setMaterialId(stock.getMaterialId());
            item.setSpecId(stock.getSpecId());
            item.setFurnaceNo(stock.getFurnaceNo());
            item.setPlanWeight(stock.getWeight() != null && stock.getQuantity() != null && stock.getQuantity() > 0
                    ? stock.getWeight().divide(BigDecimal.valueOf(stock.getQuantity())).multiply(BigDecimal.valueOf(planQty))
                    : BigDecimal.ZERO);
            item.setCostUnitPrice(stock.getCostUnitPrice());
            item.setCostAmount(stock.getCostUnitPrice() != null
                    ? stock.getCostUnitPrice().multiply(BigDecimal.valueOf(planQty))
                    : BigDecimal.ZERO);
            item.setSortNo(i + 1);

            totalQty += planQty;
            totalWeight = totalWeight.add(item.getPlanWeight() != null ? item.getPlanWeight() : BigDecimal.ZERO);
        }

        transfer.setTotalQuantity(totalQty);
        transfer.setTotalWeight(totalWeight);

        boolean updated = this.updateById(transfer);
        if (!updated) {
            return false;
        }

        transferItemService.deleteByTransferId(dto.getId());
        for (InventoryTransferItem item : items) {
            item.setId(null);
            item.setTransferId(transfer.getId());
            item.setCreateTime(now());
        }
        transferItemService.saveBatch(items);

        return true;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        InventoryTransfer transfer = this.getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new RuntimeException("只有草稿状态的调拨单可以删除");
        }

        transferItemService.deleteByTransferId(id);
        return this.removeById(id);
    }

    @Override
    @Transactional
    public boolean audit(Long id, Long userId) {
        InventoryTransfer transfer = this.getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (!"DRAFT".equals(transfer.getStatus())) {
            throw new RuntimeException("只有草稿状态的调拨单可以审核");
        }

        List<InventoryTransferItem> items = transferItemService.listByTransferId(id);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("调拨单没有明细");
        }

        for (InventoryTransferItem item : items) {
            Stock stock = stockService.getById(item.getStockId());
            if (stock == null) {
                throw new RuntimeException("库存记录不存在");
            }
            int planQty = item.getPlanQuantity() != null ? item.getPlanQuantity() : 0;
            if (planQty > (stock.getQuantity() != null ? stock.getQuantity() : 0)) {
                throw new RuntimeException("库存不足，商品：" + stock.getProductName() + "，当前库存：" + stock.getQuantity() + "，调拨数量：" + planQty);
            }
        }

        for (InventoryTransferItem item : items) {
            stockService.decreaseStock(item.getStockId(), item.getPlanQuantity(), item.getPlanWeight());

            inTransitStockService.addTransferInTransitStock(
                    item, id, transfer.getFromWarehouseId(), transfer.getToWarehouseId());
        }

        transfer.setStatus("AUDITED");
        transfer.setAuditBy(userId);
        transfer.setAuditTime(now());
        transfer.setUpdateTime(now());

        return this.updateById(transfer);
    }

    @Override
    @Transactional
    public boolean receive(Long id, Long userId, List<InventoryTransferItem> receiveItems) {
        InventoryTransfer transfer = this.getById(id);
        if (transfer == null) {
            throw new RuntimeException("调拨单不存在");
        }
        if (!"AUDITED".equals(transfer.getStatus())) {
            throw new RuntimeException("只有已审核(在途)状态的调拨单可以收货");
        }
        if (receiveItems == null || receiveItems.isEmpty()) {
            throw new RuntimeException("请填写收货明细");
        }

        List<InventoryTransferItem> originalItems = transferItemService.listByTransferId(id);
        Map<Long, InventoryTransferItem> originalItemMap = originalItems.stream()
                .collect(Collectors.toMap(InventoryTransferItem::getId, item -> item));

        int totalReceivedQty = 0;
        BigDecimal totalReceivedWeight = BigDecimal.ZERO;

        for (InventoryTransferItem receiveItem : receiveItems) {
            InventoryTransferItem originalItem = originalItemMap.get(receiveItem.getId());
            if (originalItem == null) {
                throw new RuntimeException("明细不存在");
            }

            int actualQty = receiveItem.getActualQuantity() != null ? receiveItem.getActualQuantity() : 0;
            if (actualQty <= 0) {
                throw new RuntimeException("实收数量必须大于0");
            }
            int planQty = originalItem.getPlanQuantity() != null ? originalItem.getPlanQuantity() : 0;

            BigDecimal actualWeight = receiveItem.getActualWeight();
            if (actualWeight == null) {
                if (originalItem.getPlanWeight() != null && planQty > 0) {
                    BigDecimal unitWeight = originalItem.getPlanWeight().divide(BigDecimal.valueOf(planQty), 6, BigDecimal.ROUND_HALF_UP);
                    actualWeight = unitWeight.multiply(BigDecimal.valueOf(actualQty));
                } else {
                    actualWeight = BigDecimal.ZERO;
                }
            }

            int diffQty = actualQty - planQty;
            BigDecimal diffWeight = actualWeight.subtract(originalItem.getPlanWeight() != null ? originalItem.getPlanWeight() : BigDecimal.ZERO);

            originalItem.setActualQuantity(actualQty);
            originalItem.setActualWeight(actualWeight);
            originalItem.setDiffQuantity(diffQty);
            originalItem.setDiffWeight(diffWeight);
            transferItemService.updateById(originalItem);

            inTransitStockService.releaseTransferInTransitStock(originalItem.getId(), planQty);

            stockService.increaseStock(
                    originalItem.getProductId(),
                    originalItem.getMaterialId(),
                    originalItem.getSpecId(),
                    transfer.getToWarehouseId(),
                    originalItem.getFurnaceNo(),
                    actualQty,
                    actualWeight,
                    originalItem.getCostUnitPrice()
            );

            totalReceivedQty += actualQty;
            totalReceivedWeight = totalReceivedWeight.add(actualWeight);
        }

        transfer.setReceivedQuantity(totalReceivedQty);
        transfer.setReceivedWeight(totalReceivedWeight);
        transfer.setStatus("RECEIVED");
        transfer.setReceiveBy(userId);
        transfer.setReceiveTime(now());
        transfer.setUpdateTime(now());

        return this.updateById(transfer);
    }
}
