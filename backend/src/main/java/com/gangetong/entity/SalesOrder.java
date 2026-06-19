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
@TableName("sales_order")
public class SalesOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("customer_id")
    private Long customerId;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String customerCode;

    @TableField(exist = false)
    private BigDecimal creditLimit;

    @TableField(exist = false)
    private BigDecimal currentDebt;

    @TableField("order_date")
    private String orderDate;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private String statusText;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("is_over_credit")
    private Integer isOverCredit;

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
    private List<SalesOrderItem> items;

    @TableField(exist = false)
    private List<InventoryLock> locks;
}
