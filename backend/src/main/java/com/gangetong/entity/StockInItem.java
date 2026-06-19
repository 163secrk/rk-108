package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("stock_in_item")
public class StockInItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("contract_item_id")
    private Long contractItemId;

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

    @TableField("furnace_no")
    private String furnaceNo;

    @TableField("quantity")
    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("theoretical_weight")
    private BigDecimal theoreticalWeight;

    @TableField("actual_weight")
    private BigDecimal actualWeight;

    @TableField("deviation_rate")
    private BigDecimal deviationRate;

    @TableField("amount")
    private BigDecimal amount;

    @TableField(exist = false)
    private Integer contractQuantity;

    @TableField(exist = false)
    private Integer inStockQuantity;

    @TableField(exist = false)
    private Integer remainingQuantity;

    @TableField(exist = false)
    private BigDecimal contractWeight;

    @TableField("create_time")
    private String createTime;
}
