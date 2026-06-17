package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.Material;

import java.util.List;

public interface MaterialService extends IService<Material> {

    List<Material> listAll();

    Material getById(Long id);

    boolean add(Material material);

    boolean update(Material material);

    boolean delete(Long id);
}
