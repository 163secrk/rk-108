package com.gangetong.controller;

import com.gangetong.common.Result;
import com.gangetong.dto.StockQueryDTO;
import com.gangetong.entity.Stock;
import com.gangetong.service.StockService;
import com.gangetong.vo.StockSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private StockService stockService;

    @GetMapping("/query/tree")
    public Result<List<StockSummaryVO>> queryStockTree(@RequestParam(required = false) Long warehouseId,
                                                   @RequestParam(required = false) Long materialId,
                                                   @RequestParam(required = false) Long specId) {
        StockQueryDTO dto = new StockQueryDTO();
        dto.setWarehouseId(warehouseId);
        dto.setMaterialId(materialId);
        dto.setSpecId(specId);
        List<StockSummaryVO> list = stockService.queryStockTree(dto);
        return Result.success(list);
    }

    @GetMapping("/query/detail")
    public Result<List<Stock>> queryStockDetail(@RequestParam(required = false) Long warehouseId,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) Long specId) {
        List<Stock> list;
        if (warehouseId == null && materialId == null && specId == null) {
            list = stockService.listAll();
        } else if (warehouseId != null) {
            list = stockService.listByWarehouseId(warehouseId);
        } else {
            list = stockService.listAll();
            if (materialId != null) {
                list = list.stream()
                        .filter(s -> materialId.equals(s.getMaterialId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            if (specId != null) {
                list = list.stream()
                        .filter(s -> specId.equals(s.getSpecId()))
                        .collect(java.util.stream.Collectors.toList());
            }
        }
        return Result.success(list);
    }
}
