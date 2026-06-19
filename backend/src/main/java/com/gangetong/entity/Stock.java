package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("stock")
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField(exist = false)
    private String warehouseName;

    @TableField("furnace_no")
    private String furnaceNo;

    @TableField("quantity")
    private Integer quantity;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("cost_unit_price")
    private BigDecimal costUnitPrice;

    @TableField("cost_amount")
    private BigDecimal costAmount;

    @TableField("create_time")
    private String createTime;

    @TableField("update_time")
    private String updateTime;
}
