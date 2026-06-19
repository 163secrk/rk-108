package com.gangetong.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class StockQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long warehouseId;

    private Long materialId;

    private Long specId;
}
