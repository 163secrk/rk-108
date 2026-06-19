package com.gangetong.controller;

import com.gangetong.common.Result;
import com.gangetong.dto.InventoryTransferDTO;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.InventoryTransfer;
import com.gangetong.entity.InventoryTransferItem;
import com.gangetong.service.InTransitStockService;
import com.gangetong.service.InventoryTransferService;
import com.gangetong.service.StockService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transfer")
public class InventoryTransferController {

    @Autowired
    private InventoryTransferService transferService;

    @Autowired
    private StockService stockService;

    @Autowired
    private InTransitStockService inTransitStockService;

    @GetMapping("/list")
    public Result<List<InventoryTransfer>> listTransfers(@RequestParam(required = false) String status,
                                                          @RequestParam(required = false) Long fromWarehouseId,
                                                          @RequestParam(required = false) Long toWarehouseId) {
        List<InventoryTransfer> list;
        if (status != null && !status.isEmpty()) {
            list = transferService.listByStatus(status);
        } else if (fromWarehouseId != null) {
            list = transferService.listByFromWarehouseId(fromWarehouseId);
        } else if (toWarehouseId != null) {
            list = transferService.listByToWarehouseId(toWarehouseId);
        } else {
            list = transferService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<InventoryTransfer> getTransferDetail(@PathVariable Long id) {
        InventoryTransfer transfer = transferService.getDetailById(id);
        if (transfer == null) {
            return Result.error("调拨单不存在");
        }
        return Result.success(transfer);
    }

    @GetMapping("/generate-no")
    public Result<String> generateTransferNo() {
        return Result.success(transferService.generateTransferNo());
    }

    @PostMapping
    public Result<InventoryTransfer> addTransfer(@RequestBody InventoryTransferDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            dto.setCreateBy(userId);
        }
        try {
            boolean success = transferService.add(dto);
            if (success) {
                InventoryTransfer saved = transferService.getOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InventoryTransfer>()
                                .eq(InventoryTransfer::getTransferNo, dto.getTransferNo())
                                .orderByDesc(InventoryTransfer::getId)
                                .last("limit 1"));
                if (saved != null) {
                    return Result.success("创建成功", transferService.getDetailById(saved.getId()));
                }
                return Result.success("创建成功", null);
            }
            return Result.error("创建失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<InventoryTransfer> updateTransfer(@RequestBody InventoryTransferDTO dto, HttpServletRequest request) {
        try {
            boolean success = transferService.update(dto);
            if (success) {
                return Result.success("更新成功", transferService.getDetailById(dto.getId()));
            }
            return Result.error("更新失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTransfer(@PathVariable Long id) {
        try {
            boolean success = transferService.delete(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/audit/{id}")
    public Result<InventoryTransfer> auditTransfer(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            boolean success = transferService.audit(id, userId);
            if (success) {
                return Result.success("审核成功", transferService.getDetailById(id));
            }
            return Result.error("审核失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/receive/{id}")
    public Result<InventoryTransfer> receiveTransfer(@PathVariable Long id,
                                                      @RequestBody List<InventoryTransferItem> items,
                                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            boolean success = transferService.receive(id, userId, items);
            if (success) {
                return Result.success("收货成功", transferService.getDetailById(id));
            }
            return Result.error("收货失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/in-transit/list")
    public Result<List<InTransitStock>> listInTransitStock(@RequestParam(required = false) Long toWarehouseId,
                                                            @RequestParam(required = false) Long transferId) {
        List<InTransitStock> list;
        if (transferId != null) {
            list = inTransitStockService.listByTransferId(transferId);
        } else if (toWarehouseId != null) {
            list = inTransitStockService.listByToWarehouseId(toWarehouseId);
        } else {
            list = inTransitStockService.listAll();
        }
        return Result.success(list);
    }
}
