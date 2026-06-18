package com.gangetong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gangetong.common.Result;
import com.gangetong.dto.PurchaseContractDTO;
import com.gangetong.entity.InTransitStock;
import com.gangetong.entity.PurchaseContract;
import com.gangetong.service.InTransitStockService;
import com.gangetong.service.PurchaseContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase")
public class PurchaseController {

    @Autowired
    private PurchaseContractService purchaseContractService;

    @Autowired
    private InTransitStockService inTransitStockService;

    // ========================================
    // 采购合同 PurchaseContract 接口
    // ========================================

    @GetMapping("/contract/list")
    public Result<List<PurchaseContract>> listContracts(@RequestParam(required = false) String status) {
        List<PurchaseContract> list;
        if (status != null && !status.isEmpty()) {
            list = purchaseContractService.listByStatus(status);
        } else {
            list = purchaseContractService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/contract/{id}")
    public Result<PurchaseContract> getContractDetail(@PathVariable Long id) {
        PurchaseContract contract = purchaseContractService.getDetailById(id);
        if (contract == null) {
            return Result.error("合同不存在");
        }
        return Result.success(contract);
    }

    @GetMapping("/contract/generate-no")
    public Result<String> generateContractNo() {
        String contractNo = purchaseContractService.generateContractNo();
        return Result.success(contractNo);
    }

    @PostMapping("/contract")
    public Result<PurchaseContract> addContract(@RequestBody PurchaseContractDTO dto) {
        if (dto.getSupplierId() == null) {
            return Result.error("供应商不能为空");
        }
        if (dto.getSignDate() == null || dto.getSignDate().trim().isEmpty()) {
            return Result.error("签订日期不能为空");
        }
        boolean success = purchaseContractService.add(dto);
        if (success) {
            LambdaQueryWrapper<PurchaseContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PurchaseContract::getContractNo, dto.getContractNo())
                    .orderByDesc(PurchaseContract::getId)
                    .last("limit 1");
            PurchaseContract saved = purchaseContractService.getOne(wrapper);
            if (saved != null) {
                return Result.success("创建成功", purchaseContractService.getDetailById(saved.getId()));
            }
            return Result.success("创建成功", null);
        }
        return Result.error("创建失败");
    }

    @PutMapping("/contract")
    public Result<PurchaseContract> updateContract(@RequestBody PurchaseContractDTO dto) {
        if (dto.getId() == null) {
            return Result.error("ID不能为空");
        }
        try {
            boolean success = purchaseContractService.update(dto);
            if (success) {
                return Result.success("修改成功", purchaseContractService.getDetailById(dto.getId()));
            }
            return Result.error("修改失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/contract/{id}")
    @Transactional
    public Result<Void> deleteContract(@PathVariable Long id) {
        try {
            boolean success = purchaseContractService.delete(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/contract/audit/{id}")
    public Result<PurchaseContract> auditContract(@PathVariable Long id) {
        try {
            boolean success = purchaseContractService.audit(id);
            if (success) {
                return Result.success("审核成功", purchaseContractService.getDetailById(id));
            }
            return Result.error("审核失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/contract/complete/{id}")
    public Result<PurchaseContract> completeContract(@PathVariable Long id) {
        try {
            boolean success = purchaseContractService.complete(id);
            if (success) {
                return Result.success("完成成功", purchaseContractService.getDetailById(id));
            }
            return Result.error("完成失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // ========================================
    // 在途库存 InTransitStock 接口
    // ========================================

    @GetMapping("/in-transit/list")
    public Result<List<InTransitStock>> listInTransitStock(@RequestParam(required = false) Long productId) {
        List<InTransitStock> list;
        if (productId != null) {
            list = inTransitStockService.listByProductId(productId);
        } else {
            list = inTransitStockService.listAll();
        }
        return Result.success(list);
    }
}
