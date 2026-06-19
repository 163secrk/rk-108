package com.gangetong.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class StockBatchVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String furnaceNo;

    private String stockInDate;

    private Integer remainingQuantity;

    private BigDecimal remainingWeight;

    private BigDecimal unitPrice;
}
