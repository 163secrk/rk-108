package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("sales_order_item")
public class SalesOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("product_id")
    private Long productId;

    @TableField(exist = false)
    private String productCode;

    @TableField(exist = false)
    private String productName;

    @TableField("material_id")
    private Long materialId;

    @TableField(exist = false)
    private String materialName;

    @TableField("spec_id")
    private Long specId;

    @TableField(exist = false)
    private String diameter;

    @TableField(exist = false)
    private String wallThickness;

    @TableField(exist = false)
    private String length;

    @TableField(exist = false)
    private BigDecimal weightPerMeter;

    @TableField("quantity")
    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("out_stock_quantity")
    private Integer outStockQuantity;

    @TableField(exist = false)
    private Integer remainingQuantity;

    @TableField(exist = false)
    private java.util.List<InventoryLock> stockLocks;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("create_time")
    private String createTime;
}
