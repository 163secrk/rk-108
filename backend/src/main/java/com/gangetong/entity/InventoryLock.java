package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("inventory_lock")
public class InventoryLock implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_item_id")
    private Long orderItemId;

    @TableField("stock_id")
    private Long stockId;

    @TableField("product_id")
    private Long productId;

    @TableField("warehouse_id")
    private Long warehouseId;

    @TableField(exist = false)
    private String warehouseName;

    @TableField("furnace_no")
    private String furnaceNo;

    @TableField("lock_quantity")
    private Integer lockQuantity;

    @TableField("lock_weight")
    private BigDecimal lockWeight;

    @TableField("status")
    private String status;

    @TableField("create_time")
    private String createTime;

    @TableField("release_time")
    private String releaseTime;
}
