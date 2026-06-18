package com.gangetong.dto;

import com.gangetong.entity.PurchaseContractItem;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PurchaseContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String contractNo;

    private Long supplierId;

    private String signDate;

    private String deliveryDate;

    private String status;

    private BigDecimal totalAmount;

    private BigDecimal totalWeight;

    private String remark;

    private Long createBy;

    private List<PurchaseContractItem> items;

    private List<Long> deletedItemIds;
}
