package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.entity.DictData;

import java.util.List;

public interface DictDataService extends IService<DictData> {

    List<DictData> listByDictType(String dictType);

    IPage<DictData> pageByDictType(String dictType, int pageNum, int pageSize);

    boolean addDictData(DictData dictData);

    boolean updateDictData(DictData dictData);

    boolean deleteDictData(String id);

    String getLabelByValue(String dictType, String dictValue);
}
