package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("purchase_contract_item")
public class PurchaseContractItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("contract_id")
    private Long contractId;

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

    @TableField("in_stock_quantity")
    private Integer inStockQuantity;

    @TableField(exist = false)
    private Integer remainingQuantity;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("create_time")
    private String createTime;
}
