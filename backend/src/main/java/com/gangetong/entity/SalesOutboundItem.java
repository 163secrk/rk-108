package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("sales_outbound_item")
public class SalesOutboundItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("outbound_id")
    private Long outboundId;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("lock_id")
    private Long lockId;

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

    @TableField("stock_id")
    private Long stockId;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField(exist = false)
    private String warehouseName;

    @TableField("furnace_no")
    private String furnaceNo;

    @TableField("plan_quantity")
    private Integer planQuantity;

    @TableField("actual_quantity")
    private Integer actualQuantity;

    @TableField("plan_weight")
    private BigDecimal planWeight;

    @TableField("actual_weight")
    private BigDecimal actualWeight;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("create_time")
    private String createTime;
}
