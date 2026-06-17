package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
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

    @TableField("unit")
    private String unit;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("create_time")
    private String createTime;
}
