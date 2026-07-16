package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.DictTypeQueryDTO;
import com.bcsport.admin.entity.DictType;

import java.util.List;

public interface DictTypeService extends IService<DictType> {

    List<DictType> listAll();

    /**
     * 分页查询字典类型，支持按名称/编码模糊搜索
     */
    IPage<DictType> pageDictType(DictTypeQueryDTO query, int pageNum, int pageSize);

    boolean addDictType(DictType dictType);

    boolean updateDictType(DictType dictType);

    boolean deleteDictType(String id);
}
