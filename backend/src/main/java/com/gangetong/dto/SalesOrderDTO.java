package com.gangetong.dto;

import com.gangetong.entity.InventoryLock;
import com.gangetong.entity.SalesOrderItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long customerId;

    private String orderDate;

    private String status;

    private BigDecimal totalAmount;

    private BigDecimal totalWeight;

    private Integer isOverCredit;

    private String remark;

    private Long createBy;

    private List<SalesOrderItem> items;

    private List<Long> deletedItemIds;

    private List<InventoryLock> locks;
}
