
package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeExclusionDTO;
import com.bcsport.admin.dto.IhrEmployeeExclusionQueryDTO;
import com.bcsport.admin.entity.qywx.IhrEmployeeExclusion;
import com.bcsport.admin.vo.IhrEmployeeExclusionVO;

import java.util.List;

public interface IhrEmployeeExclusionService extends IService<IhrEmployeeExclusion> {

    PageResult<IhrEmployeeExclusionVO> pageExclusions(PageQuery pageQuery, IhrEmployeeExclusionQueryDTO queryDTO);

    IhrEmployeeExclusionVO getExclusionVOById(String id);

    boolean addExclusion(IhrEmployeeExclusionDTO dto);

    boolean updateExclusion(IhrEmployeeExclusionDTO dto);

    boolean deleteExclusion(String id);

    boolean batchDelete(List<String> ids);

    boolean batchUpdateStatus(List<String> ids, Integer targetStatus);

    boolean checkExcluded(String staffName, String staffNo, Integer exclusionType);
}
