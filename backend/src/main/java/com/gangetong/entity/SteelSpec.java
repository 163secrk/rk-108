package com.gangetong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("steel_spec")
public class SteelSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("material_id")
    private Long materialId;

    @TableField(exist = false)
    private String materialName;

    @TableField("diameter")
    private String diameter;

    @TableField("wall_thickness")
    private String wallThickness;

    @TableField("length")
    private String length;

    @TableField("weight_per_meter")
    private BigDecimal weightPerMeter;

    @TableField("create_time")
    private String createTime;
}
