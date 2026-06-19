package com.gangetong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gangetong.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Select("SELECT MIN(o.create_time) FROM stock_in_item i " +
            "LEFT JOIN stock_in_order o ON i.order_id = o.id " +
            "WHERE i.product_id = #{productId} " +
            "AND i.furnace_no = #{furnaceNo} " +
            "AND o.status = 'AUDITED'")
    String getStockInDate(@Param("productId") Long productId, @Param("furnaceNo") String furnaceNo);
}
