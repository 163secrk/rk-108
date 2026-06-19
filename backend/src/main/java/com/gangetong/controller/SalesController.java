package com.gangetong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gangetong.common.Result;
import com.gangetong.dto.SalesOrderDTO;
import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.SalesOrder;
import com.gangetong.entity.Stock;
import com.gangetong.service.InventoryLockService;
import com.gangetong.service.SalesOrderService;
import com.gangetong.service.StockService;
import com.gangetong.vo.StockBatchVO;
import com.gangetong.vo.StockSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private StockService stockService;

    @Autowired
    private InventoryLockService inventoryLockService;

    // ========================================
    // 销售订单 SalesOrder 接口
    // ========================================

    @GetMapping("/order/list")
    public Result<List<SalesOrder>> listOrders(@RequestParam(required = false) String status) {
        List<SalesOrder> list;
        if (status != null && !status.isEmpty()) {
            list = salesOrderService.listByStatus(status);
        } else {
            list = salesOrderService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/order/{id}")
    public Result<SalesOrder> getOrderDetail(@PathVariable Long id) {
        SalesOrder order = salesOrderService.getDetailById(id);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/order/generate-no")
    public Result<String> generateOrderNo() {
        String orderNo = salesOrderService.generateOrderNo();
        return Result.success(orderNo);
    }

    @PostMapping("/order")
    public Result<SalesOrder> addOrder(@RequestBody SalesOrderDTO dto) {
        if (dto.getCustomerId() == null) {
            return Result.error("客户不能为空");
        }
        if (dto.getOrderDate() == null || dto.getOrderDate().trim().isEmpty()) {
            return Result.error("订单日期不能为空");
        }
        boolean success = salesOrderService.add(dto);
        if (success) {
            LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SalesOrder::getOrderNo, dto.getOrderNo())
                    .orderByDesc(SalesOrder::getId)
                    .last("limit 1");
            SalesOrder saved = salesOrderService.getOne(wrapper);
            if (saved != null) {
                return Result.success("创建成功", salesOrderService.getDetailById(saved.getId()));
            }
            return Result.success("创建成功", null);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/order")
    public Result<SalesOrder> updateOrder(@RequestBody SalesOrderDTO dto) {
        if (dto.getId() == null) {
            return Result.error("ID不能为空");
        }
        try {
            boolean success = salesOrderService.update(dto);
            if (success) {
                return Result.success("修改成功", salesOrderService.getDetailById(dto.getId()));
            }
            return Result.error("修改失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/order/{id}")
    @Transactional
    public Result<Void> deleteOrder(@PathVariable Long id) {
        try {
            boolean success = salesOrderService.delete(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/order/confirm/{id}")
    @Transactional
    public Result<Map<String, Object>> confirmOrder(@PathVariable Long id) {
        try {
            Map<String, Object> result = salesOrderService.confirm(id);
            Boolean success = (Boolean) result.get("success");
            if (Boolean.TRUE.equals(success)) {
                return Result.success((String) result.get("message"), result);
            } else {
                return Result.error((String) result.get("message"));
            }
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/order/check-credit")
    public Result<Map<String, Object>> checkCredit(
            @RequestParam Long customerId,
            @RequestParam(required = false) BigDecimal orderAmount) {
        Map<String, Object> result = salesOrderService.checkCredit(customerId, orderAmount);
        return Result.success(result);
    }

    // ========================================
    // 库存选择器：查询可用库存（已扣除锁定量）
    // ========================================

    @GetMapping("/stock/available")
    public Result<List<Stock>> listAvailableStock(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long specId,
            @RequestParam(required = false) Long excludeOrderId) {
        // 使用库存树查询（带锁定扣除），然后扁平化
        List<StockSummaryVO> tree = stockService.queryStockTreeForSaleWithLock(warehouseId, materialId, specId, productId, excludeOrderId);
        List<Stock> result = new ArrayList<>();
        for (StockSummaryVO summary : tree) {
            if (summary.getChildren() == null) continue;
            for (StockBatchVO batch : summary.getChildren()) {
                Stock s = new Stock();
                s.setId(batch.getId());
                s.setProductId(summary.getProductId());
                s.setProductName(summary.getProductName());
                s.setMaterialId(summary.getMaterialId());
                s.setMaterialName(summary.getMaterialName());
                s.setSpecId(summary.getSpecId());
                s.setSpecText(summary.getSpecText());
                s.setWarehouseId(summary.getWarehouseId());
                s.setWarehouseName(summary.getWarehouseName());
                s.setFurnaceNo(batch.getFurnaceNo());
                s.setQuantity(batch.getRemainingQuantity());
                s.setWeight(batch.getRemainingWeight());
                s.setCostUnitPrice(batch.getUnitPrice());
                s.setStockInDate(batch.getStockInDate());
                result.add(s);
            }
        }
        return Result.success(result);
    }

    @GetMapping("/stock/available-tree")
    public Result<List<StockSummaryVO>> queryAvailableStockTree(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) Long specId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long excludeOrderId) {
        List<StockSummaryVO> tree = stockService.queryStockTreeForSaleWithLock(
                warehouseId, materialId, specId, productId, excludeOrderId);
        return Result.success(tree);
    }
}
