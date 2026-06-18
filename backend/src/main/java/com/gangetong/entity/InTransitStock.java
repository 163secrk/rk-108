package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("in_transit_stock")
public class InTransitStock implements Serializable {

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

    @TableField("contract_id")
    private Long contractId;

    @TableField(exist = false)
    private String contractNo;

    @TableField("contract_item_id")
    private Long contractItemId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("weight")
    private BigDecimal weight;

    @TableField("create_time")
    private String createTime;
}
