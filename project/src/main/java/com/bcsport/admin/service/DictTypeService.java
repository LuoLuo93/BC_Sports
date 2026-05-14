package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.entity.DictType;

import java.util.List;

public interface DictTypeService extends IService<DictType> {

    List<DictType> listAll();

    boolean addDictType(DictType dictType);

    boolean updateDictType(DictType dictType);

    boolean deleteDictType(String id);
}
