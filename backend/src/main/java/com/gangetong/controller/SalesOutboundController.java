package com.gangetong.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gangetong.common.Result;
import com.gangetong.dto.SalesOutboundDTO;
import com.gangetong.entity.SalesOutbound;
import com.gangetong.service.SalesOutboundService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-outbound")
public class SalesOutboundController {

    @Autowired
    private SalesOutboundService salesOutboundService;

    @GetMapping("/list")
    public Result<List<SalesOutbound>> list(@RequestParam(required = false) String status) {
        List<SalesOutbound> list;
        if (status != null && !status.isEmpty()) {
            list = salesOutboundService.listByStatus(status);
        } else {
            list = salesOutboundService.listAll();
        }
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<SalesOutbound> getDetail(@PathVariable Long id) {
        SalesOutbound outbound = salesOutboundService.getDetailById(id);
        if (outbound == null) {
            return Result.error("出库单不存在");
        }
        return Result.success(outbound);
    }

    @GetMapping("/generate-no")
    public Result<String> generateOutboundNo() {
        return Result.success(salesOutboundService.generateOutboundNo());
    }

    @GetMapping("/generate-from-order/{orderId}")
    public Result<SalesOutbound> generateFromOrder(@PathVariable Long orderId) {
        try {
            SalesOutbound outbound = salesOutboundService.generateFromOrder(orderId);
            return Result.success(outbound);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping
    public Result<SalesOutbound> add(@RequestBody SalesOutboundDTO dto, HttpServletRequest request) {
        if (dto.getOrderId() == null) {
            return Result.error("请选择销售订单");
        }
        Long userId = (Long) request.getAttribute("userId");
        if (userId != null) {
            dto.setCreateBy(userId);
        }
        try {
            boolean success = salesOutboundService.add(dto);
            if (success) {
                SalesOutbound saved = salesOutboundService.getOne(
                        new LambdaQueryWrapper<SalesOutbound>()
                                .eq(SalesOutbound::getOutboundNo, dto.getOutboundNo())
                                .orderByDesc(SalesOutbound::getId)
                                .last("limit 1"));
                if (saved != null) {
                    return Result.success("创建成功", salesOutboundService.getDetailById(saved.getId()));
                }
                return Result.success("创建成功", null);
            }
            return Result.error("创建失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<SalesOutbound> update(@RequestBody SalesOutboundDTO dto) {
        if (dto.getId() == null) {
            return Result.error("ID不能为空");
        }
        try {
            boolean success = salesOutboundService.update(dto);
            if (success) {
                return Result.success("修改成功", salesOutboundService.getDetailById(dto.getId()));
            }
            return Result.error("修改失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        try {
            boolean success = salesOutboundService.delete(id);
            if (success) {
                return Result.success("删除成功", null);
            }
            return Result.error("删除失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/audit/{id}")
    public Result<SalesOutbound> audit(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        try {
            boolean success = salesOutboundService.audit(id, userId);
            if (success) {
                return Result.success("审核成功", salesOutboundService.getDetailById(id));
            }
            return Result.error("审核失败");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
