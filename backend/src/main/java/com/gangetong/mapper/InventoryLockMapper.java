package com.gangetong.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gangetong.entity.InventoryLock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InventoryLockMapper extends BaseMapper<InventoryLock> {

    @Select("SELECT COALESCE(SUM(lock_quantity), 0) FROM inventory_lock WHERE stock_id = #{stockId} AND status = 'LOCKED'")
    Integer sumLockedQuantityByStockId(@Param("stockId") Long stockId);

    @Select("SELECT COALESCE(SUM(lock_quantity), 0) FROM inventory_lock WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} AND furnace_no = #{furnaceNo} AND status = 'LOCKED' AND order_id != #{excludeOrderId}")
    Integer sumLockedQuantityByBatch(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("furnaceNo") String furnaceNo,
            @Param("excludeOrderId") Long excludeOrderId
    );

    @Select("SELECT * FROM inventory_lock WHERE order_item_id = #{orderItemId} AND status = 'LOCKED'")
    List<InventoryLock> listByOrderItemId(@Param("orderItemId") Long orderItemId);

    @Select("SELECT * FROM inventory_lock WHERE order_id = #{orderId} AND status = 'LOCKED'")
    List<InventoryLock> listByOrderId(@Param("orderId") Long orderId);
}
