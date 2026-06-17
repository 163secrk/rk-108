package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.SteelSpec;

import java.util.List;

public interface SteelSpecService extends IService<SteelSpec> {

    List<SteelSpec> listByMaterialId(Long materialId);

    List<SteelSpec> listAll();

    SteelSpec getById(Long id);

    boolean add(SteelSpec spec);

    boolean update(SteelSpec spec);

    boolean delete(Long id);

    boolean deleteByMaterialId(Long materialId);
}
