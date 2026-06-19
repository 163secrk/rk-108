package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("inventory_transfer_item")
public class InventoryTransferItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("transfer_id")
    private Long transferId;

    @TableField("stock_id")
    private Long stockId;

    @TableField("product_id")
    private Long productId;

    @TableField(exist = false)
    private String productName;

    @TableField("material_id")
    private Long materialId;

    @TableField(exist = false)
    private String materialName;

    @TableField("spec_id")
    private Long specId;

    @TableField(exist = false)
    private String specText;

    @TableField("furnace_no")
    private String furnaceNo;

    @TableField("plan_quantity")
    private Integer planQuantity;

    @TableField("plan_weight")
    private BigDecimal planWeight;

    @TableField("actual_quantity")
    private Integer actualQuantity;

    @TableField("actual_weight")
    private BigDecimal actualWeight;

    @TableField("diff_quantity")
    private Integer diffQuantity;

    @TableField("diff_weight")
    private BigDecimal diffWeight;

    @TableField("cost_unit_price")
    private BigDecimal costUnitPrice;

    @TableField("cost_amount")
    private BigDecimal costAmount;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("create_time")
    private String createTime;
}
