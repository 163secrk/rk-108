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
@TableName("sales_outbound")
public class SalesOutbound implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("outbound_no")
    private String outboundNo;

    @TableField("order_id")
    private Long orderId;

    @TableField(exist = false)
    private String orderNo;

    @TableField("customer_id")
    private Long customerId;

    @TableField(exist = false)
    private String customerName;

    @TableField(exist = false)
    private String customerCode;

    @TableField("status")
    private String status;

    @TableField(exist = false)
    private String statusText;

    @TableField("total_quantity")
    private Integer totalQuantity;

    @TableField("total_weight")
    private BigDecimal totalWeight;

    @TableField("plate_no")
    private String plateNo;

    @TableField("driver_name")
    private String driverName;

    @TableField("driver_phone")
    private String driverPhone;

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

    @TableField(exist = false)
    private List<SalesOutboundItem> items;
}
