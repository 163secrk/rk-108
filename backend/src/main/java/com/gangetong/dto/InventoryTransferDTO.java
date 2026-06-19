package com.gangetong.dto;

import com.gangetong.entity.InventoryTransferItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InventoryTransferDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String transferNo;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    private String status;

    private Integer totalQuantity;

    private BigDecimal totalWeight;

    private Integer receivedQuantity;

    private BigDecimal receivedWeight;

    private String remark;

    private Long createBy;

    private Long auditBy;

    private Long receiveBy;

    private List<InventoryTransferItem> items;
}
