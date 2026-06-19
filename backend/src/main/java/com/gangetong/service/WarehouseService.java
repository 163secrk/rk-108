package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.Warehouse;

import java.util.List;

public interface WarehouseService extends IService<Warehouse> {

    List<Warehouse> listAll();

    List<Warehouse> listByParentId(Long parentId);

    List<Warehouse> treeList();

    Warehouse getById(Long id);

    boolean add(Warehouse warehouse);

    boolean update(Warehouse warehouse);

    boolean delete(Long id);

    List<Long> listAllChildIds(Long warehouseId);
}
