package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    List<Product> listAll();

    List<Product> listByMaterialId(Long materialId);

    Product getById(Long id);

    boolean add(Product product);

    boolean update(Product product);

    boolean delete(Long id);

    boolean deleteByMaterialId(Long materialId);
}
