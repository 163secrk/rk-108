package com.gangetong.dto;

import com.gangetong.entity.SalesOutboundItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SalesOutboundDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String outboundNo;

    private Long orderId;

    private Long customerId;

    private String status;

    private Integer totalQuantity;

    private BigDecimal totalWeight;

    private String plateNo;

    private String driverName;

    private String driverPhone;

    private String remark;

    private Long createBy;

    private List<SalesOutboundItem> items;

    private List<Long> deletedItemIds;
}
