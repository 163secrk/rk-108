package com.gangetong.controller;

import com.gangetong.common.Result;
import com.gangetong.dto.StockInOrderDTO;
import com.gangetong.entity.Stock;
import com.gangetong.entity.StockInOrder;
import com.gangetong.service.StockInOrderService;
import com.gangetong.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-in")
public class StockInController {

    @Autowired
    private StockInOrderService stockInOrderService;

    @Autowired
    private StockService stockService;

    @GetMapping("/order/list")
    public Result<List<StockInOrder>> listOrders(@RequestParam(required = false) String status) {
        List<StockInOrder> list;
        if (status != null && !status.isEmpty()) {
            list = stockInOrderService.listByStatus(status);
        } else {
            list = stockInOrderService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/order/{id}")
    public Result<StockInOrder> getOrderDetail(@PathVariable Long id) {
        StockInOrder order = stockInOrderService.getDetailById(id);
        if (order == null) {
            return Result.error("入库单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/order/generate-no")
    public Result<String> generateOrderNo() {
        return Result.success(stockInOrderService.generateOrderNo());
    }

    @PostMapping("/order")
    public Result<StockInOrder> addOrder(@RequestBody StockInOrderDTO dto, HttpServletRequest request) {
        if (dto.getContractId() == null) {
            return Result.error("请选择采购合同");
        }
        if (dto.getWarehouseId() == null) {
            return Result.error("请选择入库仓库");
        }
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            dto.setCreateBy(userId);
        }
        try {
            boolean success = stockInOrderService.add(dto);
            if (success) {
                StockInOrder saved = stockInOrderService.getOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockInOrder>()
                        .eq(StockInOrder::getOrderNo, dto.getOrderNo())
                        .orderByDesc(StockInOrder::getId)
                        .last("limit 1"));
                if (saved != null) {
                    return Result.success("创建成功", stockInOrderService.getDetailById(saved.getId()));
                }
                return Result.success("创建成功", null);
            }
            return Result.error("创建失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/order/{id}")
    @Transactional
    public Result<Void> deleteOrder(@PathVariable Long id) {
        try {
            boolean success = stockInOrderService.delete(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/order/audit/{id}")
    public Result<StockInOrder> auditOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            boolean success = stockInOrderService.audit(id, userId);
            if (success) {
                return Result.success("审核成功", stockInOrderService.getDetailById(id));
            }
            return Result.error("审核失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stock/list")
    public Result<List<Stock>> listStock(@RequestParam(required = false) Long productId,
                                          @RequestParam(required = false) Long warehouseId) {
        List<Stock> list;
        if (productId != null) {
            list = stockService.listByProductId(productId);
        } else if (warehouseId != null) {
            list = stockService.listByWarehouseId(warehouseId);
        } else {
            list = stockService.listAll();
        }
        return Result.success(list);
    }
}
