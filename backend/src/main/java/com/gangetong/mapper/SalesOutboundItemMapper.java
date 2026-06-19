package com.gangetong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gangetong.entity.SalesOutboundItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalesOutboundItemMapper extends BaseMapper<SalesOutboundItem> {

    @Select("SELECT * FROM sales_outbound_item WHERE outbound_id = #{outboundId} ORDER BY sort_no, id")
    List<SalesOutboundItem> listByOutboundId(@Param("outboundId") Long outboundId);
}
