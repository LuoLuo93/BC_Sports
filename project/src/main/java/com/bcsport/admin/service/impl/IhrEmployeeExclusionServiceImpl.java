
package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeExclusionDTO;
import com.bcsport.admin.dto.IhrEmployeeExclusionQueryDTO;
import com.bcsport.admin.entity.qywx.IhrEmployeeExclusion;
import com.bcsport.admin.qywxmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.IhrEmployeeExclusionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IhrEmployeeExclusionServiceImpl extends ServiceImpl&lt;IhrEmployeeExclusionMapper, IhrEmployeeExclusion&gt; implements IhrEmployeeExclusionService {

    @Override
    public PageResult&lt;IhrEmployeeExclusionVO&gt; pageExclusions(PageQuery pageQuery, IhrEmployeeExclusionQueryDTO queryDTO) {
        Page&lt;IhrEmployeeExclusion&gt; page = pageQuery.toPage();

        LambdaQueryWrapper&lt;IhrEmployeeExclusion&gt; queryWrapper = new LambdaQueryWrapper&lt;&gt;();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getStaffName())) {
                queryWrapper.like(IhrEmployeeExclusion::getStaffName, queryDTO.getStaffName());
            }
            if (StringUtils.hasText(queryDTO.getStaffNo())) {
                queryWrapper.like(IhrEmployeeExclusion::getStaffNo, queryDTO.getStaffNo());
            }
            if (queryDTO.getExclusionType() != null) {
                queryWrapper.eq(IhrEmployeeExclusion::getExclusionType, queryDTO.getExclusionType());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(IhrEmployeeExclusion::getStatus, queryDTO.getStatus());
            }
        }
        queryWrapper.eq(IhrEmployeeExclusion::getDeleted, 0);

        // 默认按创建时间倒序
        if (page.orders().isEmpty()) {
            queryWrapper.orderByDesc(IhrEmployeeExclusion::getCreateTime);
        }

        Page&lt;IhrEmployeeExclusion&gt; resultPage = baseMapper.selectPage(page, queryWrapper);
        return BeanCopyUtils.copyPage(PageResult.of(resultPage), IhrEmployeeExclusionVO.class);
    }

    @Override
    public IhrEmployeeExclusionVO getExclusionVOById(String id) {
        IhrEmployeeExclusion entity = getById(id);
        return BeanCopyUtils.copy(entity, IhrEmployeeExclusionVO.class);
    }

    @Override
    public boolean addExclusion(IhrEmployeeExclusionDTO dto) {
        IhrEmployeeExclusion entity = BeanCopyUtils.copy(dto, IhrEmployeeExclusion.class);
        // 默认启用状态
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        return save(entity);
    }

    @Override
    public boolean updateExclusion(IhrEmployeeExclusionDTO dto) {
        IhrEmployeeExclusion entity = BeanCopyUtils.copy(dto, IhrEmployeeExclusion.class);
        return updateById(entity);
    }

    @Override
    public boolean deleteExclusion(String id) {
        return removeById(id);
    }

    @Override
    public boolean checkExcluded(String staffName, String staffNo, Integer exclusionType) {
        return baseMapper.checkExcluded(staffName, staffNo, exclusionType);
    }
}

