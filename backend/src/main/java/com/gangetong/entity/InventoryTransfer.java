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
@TableName("inventory_transfer")
public class InventoryTransfer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("transfer_no")
    private String transferNo;

    @TableField("from_warehouse_id")
    private Long fromWarehouseId;

    @TableField(exist = false)
    private String fromWarehouseName;

    @TableField("to_warehouse_id")
    private Long toWarehouseId;

    @TableField(exist = false)
    private String toWarehouseName;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private String statusText;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("received_quantity")
    private Integer receivedQuantity;

    @TableField("received_weight")
    private BigDecimal receivedWeight;

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

    @TableField("receive_by")
    private Long receiveBy;

    @TableField(exist = false)
    private String receiveByName;

    @TableField("receive_time")
    private String receiveTime;

    @TableField(exist = false)
    private List<InventoryTransferItem> items;
}
