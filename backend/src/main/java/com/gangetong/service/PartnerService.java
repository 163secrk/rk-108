package com.gangetong.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gangetong.entity.Partner;

import java.util.List;

public interface PartnerService extends IService<Partner> {

    List<Partner> listAll();

    List<Partner> listByType(String type);

    Partner getById(Long id);

    boolean add(Partner partner);

    boolean update(Partner partner);

    boolean delete(Long id);
}
