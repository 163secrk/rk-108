package com.gangetong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gangetong.entity.Warehouse;
import com.gangetong.mapper.WarehouseMapper;
import com.gangetong.service.WarehouseService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WarehouseServiceImpl extends ServiceImpl<WarehouseMapper, Warehouse> implements WarehouseService {

    @Override
    public List<Warehouse> listAll() {
        return this.list(new LambdaQueryWrapper<Warehouse>().orderByAsc(Warehouse::getLevel, Warehouse::getSortNo));
    }

    @Override
    public List<Warehouse> listByParentId(Long parentId) {
        return this.list(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getParentId, parentId)
                .orderByAsc(Warehouse::getSortNo));
    }

    @Override
    public List<Warehouse> treeList() {
        List<Warehouse> all = listAll();
        Map<Long, String> nameMap = all.stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        for (Warehouse w : all) {
            if (w.getParentId() != null && w.getParentId() > 0) {
                w.setParentName(nameMap.getOrDefault(w.getParentId(), ""));
            }
        }
        Map<Long, List<Warehouse>> childrenMap = all.stream()
                .filter(w -> w.getParentId() != null && w.getParentId() > 0)
                .collect(Collectors.groupingBy(Warehouse::getParentId));
        List<Warehouse> roots = all.stream()
                .filter(w -> w.getParentId() == null || w.getParentId() == 0)
                .collect(Collectors.toList());
        buildTree(roots, childrenMap);
        return roots;
    }

    private void buildTree(List<Warehouse> nodes, Map<Long, List<Warehouse>> childrenMap) {
        if (nodes == null) return;
        for (Warehouse node : nodes) {
            List<Warehouse> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
            node.setChildren(children);
            buildTree(children, childrenMap);
        }
    }

    @Override
    public Warehouse getById(Long id) {
        return super.getById(id);
    }

    @Override
    public boolean add(Warehouse warehouse) {
        if (warehouse.getCreateTime() == null || warehouse.getCreateTime().isEmpty()) {
            warehouse.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }
        if (warehouse.getStatus() == null) {
            warehouse.setStatus(1);
        }
        if (warehouse.getParentId() == null) {
            warehouse.setParentId(0L);
        }
        if (warehouse.getLevel() == null) {
            warehouse.setLevel(1);
        }
        return this.save(warehouse);
    }

    @Override
    public boolean update(Warehouse warehouse) {
        return this.updateById(warehouse);
    }

    @Override
    public boolean delete(Long id) {
        List<Warehouse> children = this.list(new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getParentId, id));
        for (Warehouse child : children) {
            delete(child.getId());
        }
        return this.removeById(id);
    }

    @Override
    public List<Long> listAllChildIds(Long warehouseId) {
        List<Long> ids = new ArrayList<>();
        ids.add(warehouseId);
        collectChildIds(warehouseId, ids);
        return ids;
    }

    private void collectChildIds(Long parentId, List<Long> ids) {
        List<Warehouse> children = this.list(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getParentId, parentId));
        for (Warehouse child : children) {
            ids.add(child.getId());
            collectChildIds(child.getId(), ids);
        }
    }
}
