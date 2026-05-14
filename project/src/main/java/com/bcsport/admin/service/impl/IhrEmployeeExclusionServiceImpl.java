
package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.IhrEmployeeExclusionDTO;
import com.bcsport.admin.dto.IhrEmployeeExclusionQueryDTO;
import com.bcsport.admin.entity.ihr.IhrEmployeeExclusion;
import com.bcsport.admin.ihrmapper.IhrEmployeeExclusionMapper;
import com.bcsport.admin.service.IhrEmployeeExclusionService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.IhrEmployeeExclusionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
public class IhrEmployeeExclusionServiceImpl extends ServiceImpl<IhrEmployeeExclusionMapper, IhrEmployeeExclusion> implements IhrEmployeeExclusionService {

    @Override
    public PageResult<IhrEmployeeExclusionVO> pageExclusions(PageQuery pageQuery, IhrEmployeeExclusionQueryDTO queryDTO) {
        Page<IhrEmployeeExclusion> page = pageQuery.toPage();

        LambdaQueryWrapper<IhrEmployeeExclusion> queryWrapper = new LambdaQueryWrapper<>();
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
        // @TableLogic 自动追加 deleted=0 条件，无需手动添加

        // 默认按创建时间倒序
        if (page.orders().isEmpty()) {
            queryWrapper.orderByDesc(IhrEmployeeExclusion::getCreateTime);
        }

        Page<IhrEmployeeExclusion> resultPage = baseMapper.selectPage(page, queryWrapper);
        return BeanCopyUtils.copyPage(PageResult.of(resultPage), IhrEmployeeExclusionVO.class);
    }

    @Override
    public IhrEmployeeExclusionVO getExclusionVOById(String id) {
        IhrEmployeeExclusion entity = getById(id);
        return BeanCopyUtils.copy(entity, IhrEmployeeExclusionVO.class);
    }

    @Override
    public boolean addExclusion(IhrEmployeeExclusionDTO dto) {
        // 重复校验：同姓名+工号+类型不允许重复
        LambdaQueryWrapper<IhrEmployeeExclusion> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(IhrEmployeeExclusion::getStaffName, dto.getStaffName())
                .eq(IhrEmployeeExclusion::getStaffNo, dto.getStaffNo())
                .eq(IhrEmployeeExclusion::getExclusionType, dto.getExclusionType());
        if (count(dupCheck) > 0) {
            throw new IllegalArgumentException(
                    String.format("员工 %s(%s) 在该排除类型下已存在", dto.getStaffName(), dto.getStaffNo()));
        }

        IhrEmployeeExclusion entity = BeanCopyUtils.copy(dto, IhrEmployeeExclusion.class);
        // 默认启用状态
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        return save(entity);
    }

    @Override
    public boolean updateExclusion(IhrEmployeeExclusionDTO dto) {
        IhrEmployeeExclusion existing = getById(dto.getId());
        if (existing == null) {
            throw new IllegalArgumentException("记录不存在");
        }

        // 如果修改了关键字段，校验重复
        boolean nameChanged = !java.util.Objects.equals(existing.getStaffName(), dto.getStaffName());
        boolean noChanged = !java.util.Objects.equals(existing.getStaffNo(), dto.getStaffNo());
        boolean typeChanged = !java.util.Objects.equals(existing.getExclusionType(), dto.getExclusionType());
        if (nameChanged || noChanged || typeChanged) {
            LambdaQueryWrapper<IhrEmployeeExclusion> dupCheck = new LambdaQueryWrapper<>();
            dupCheck.eq(IhrEmployeeExclusion::getStaffName, dto.getStaffName())
                    .eq(IhrEmployeeExclusion::getStaffNo, dto.getStaffNo())
                    .eq(IhrEmployeeExclusion::getExclusionType, dto.getExclusionType());
            if (count(dupCheck) > 0) {
                throw new IllegalArgumentException(
                        String.format("员工 %s(%s) 在该排除类型下已存在", dto.getStaffName(), dto.getStaffNo()));
            }
        }

        IhrEmployeeExclusion entity = BeanCopyUtils.copy(dto, IhrEmployeeExclusion.class);
        return updateById(entity);
    }

    @Override
    public boolean deleteExclusion(String id) {
        // @TableLogic 使 removeById 自动执行逻辑删除
        return removeById(id);
    }

    @Override
    public boolean batchDelete(List<String> ids) {
        return removeByIds(ids);
    }

    @Override
    public boolean batchUpdateStatus(List<String> ids, Integer targetStatus) {
        LambdaUpdateWrapper<IhrEmployeeExclusion> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(IhrEmployeeExclusion::getId, ids)
                .set(IhrEmployeeExclusion::getStatus, targetStatus);
        return update(updateWrapper);
    }

    @Override
    public boolean checkExcluded(String staffName, String staffNo, Integer exclusionType) {
        return baseMapper.checkExcluded(staffName, staffNo, exclusionType);
    }
}
