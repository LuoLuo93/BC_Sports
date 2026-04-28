package com.bcsport.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.RegionDTO;
import com.bcsport.admin.dto.RegionQueryDTO;
import com.bcsport.admin.entity.Region;
import com.bcsport.admin.vo.RegionVO;

import java.util.List;

public interface RegionService extends IService<Region> {

    List<RegionVO> listByTree(RegionQueryDTO query);

    IPage<RegionVO> listByPage(RegionQueryDTO query);

    RegionVO getRegionVOById(String id);

    void addRegion(RegionDTO regionDTO);

    void updateRegion(RegionDTO regionDTO);

    void deleteRegion(String id);

    Long getNextId();
}
