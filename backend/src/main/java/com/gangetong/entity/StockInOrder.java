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
@TableName("stock_in_order")
public class StockInOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("contract_id")
    private Long contractId;

    @TableField(exist = false)
    private String contractNo;

    @TableField("supplier_id")
    private Long supplierId;

    @TableField(exist = false)
    private String supplierName;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField(exist = false)
    private String warehouseName;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private String statusText;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("total_theoretical_weight")
    private BigDecimal totalTheoreticalWeight;

    @TableField("total_actual_weight")
    private BigDecimal totalActualWeight;

    @TableField("total_amount")
    private BigDecimal totalAmount;

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

    @TableField("audit_by")
    private Long auditBy;

    @TableField(exist = false)
    private String auditByName;

    @TableField("audit_time")
    private String auditTime;

    @TableField(exist = false)
    private List<StockInItem> items;
}
