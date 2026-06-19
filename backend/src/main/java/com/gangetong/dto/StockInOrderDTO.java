package com.gangetong.dto;

import com.gangetong.entity.StockInItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StockInOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long contractId;

    private Long supplierId;

    private Long warehouseId;

    private String status;

    private Integer totalQuantity;

    private BigDecimal totalTheoreticalWeight;

    private BigDecimal totalActualWeight;

    private BigDecimal totalAmount;

    private String remark;

    private Long createBy;

    private Long auditBy;

    private List<StockInItem> items;
}
