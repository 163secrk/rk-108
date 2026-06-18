package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("purchase_contract")
public class PurchaseContract implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("contract_no")
    private String contractNo;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String supplierCode;

    @TableField("sign_date")
    private String signDate;

    @TableField("delivery_date")
    private String deliveryDate;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private String statusText;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("remark")
    private String remark;

    @TableField("create_by")
    private Long createBy;

    @TableField(exist = false)
    private String createByName;

    @TableField("create_time")
    private String createTime;

    @TableField("update_time")
    private String updateTime;

    @TableField(exist = false)
    private List<PurchaseContractItem> items;
}
