
package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeExclusionDTO;
import com.bcsport.admin.dto.IhrEmployeeExclusionQueryDTO;
import com.bcsport.admin.entity.qywx.IhrEmployeeExclusion;
import com.bcsport.admin.vo.IhrEmployeeExclusionVO;

public interface IhrEmployeeExclusionService extends IService&lt;IhrEmployeeExclusion&gt; {

    /**
     * 分页查询
     */
    PageResult&lt;IhrEmployeeExclusionVO&gt; pageExclusions(PageQuery pageQuery, IhrEmployeeExclusionQueryDTO queryDTO);

    /**
     * 根据ID查询
     */
    IhrEmployeeExclusionVO getExclusionVOById(String id);

    /**
     * 新增
     */
    boolean addExclusion(IhrEmployeeExclusionDTO dto);

    /**
     * 更新
     */
    boolean updateExclusion(IhrEmployeeExclusionDTO dto);

    /**
     * 删除
     */
    boolean deleteExclusion(String id);

    /**
     * 检查是否在排除列表中
     */
    boolean checkExcluded(String staffName, String staffNo, Integer exclusionType);
}

