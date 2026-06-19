package com.gangetong.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long warehouseId;

    private String warehouseName;

    private Long materialId;

    private String materialName;

    private Long specId;

    private String specText;

    private Long productId;

    private String productName;

    private Integer totalQuantity;

    private BigDecimal totalWeight;

    private List<StockBatchVO> children;
}
