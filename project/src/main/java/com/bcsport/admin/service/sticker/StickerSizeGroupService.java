package com.bcsport.admin.service.sticker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.entity.sticker.StickerSize;
import com.bcsport.admin.entity.sticker.StickerSizeGroup;
import com.bcsport.admin.mapper.sticker.StickerSizeGroupMapper;
import com.bcsport.admin.mapper.sticker.StickerSizeMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 贴纸本地尺码组 Service
 */
@Service
public class StickerSizeGroupService {

    @Autowired
    private StickerSizeGroupMapper groupMapper;

    @Autowired
    private StickerSizeMapper sizeMapper;

    /**
     * 分页查询(支持 brandId/kindId/status/groupCode/groupName 筛选)
     */
    public PageResult<StickerSizeGroup> page(PageQuery pageQuery, String brandId, String kindId,
                                             Integer status, String groupCode, String groupName) {
        LambdaQueryWrapper<StickerSizeGroup> wrapper = new LambdaQueryWrapper<StickerSizeGroup>()
                .eq(brandId != null && !brandId.isBlank(), StickerSizeGroup::getBrandId, brandId)
                .eq(kindId != null && !kindId.isBlank(), StickerSizeGroup::getKindId, kindId)
                .eq(status != null, StickerSizeGroup::getStatus, status)
                .like(groupCode != null && !groupCode.isBlank(), StickerSizeGroup::getGroupCode, groupCode)
                .like(groupName != null && !groupName.isBlank(), StickerSizeGroup::getGroupName, groupName)
                .orderByAsc(StickerSizeGroup::getSort)
                .orderByDesc(StickerSizeGroup::getCreateTime);
        Page<StickerSizeGroup> page = groupMapper.selectPage(pageQuery.toPage(), wrapper);
        return PageResult.of(page);
    }

    /**
     * 按品牌+类别查启用组列表(供明细行下拉)
     */
    public List<StickerSizeGroup> listActiveByBrandKind(String brandId, String kindId) {
        LambdaQueryWrapper<StickerSizeGroup> wrapper = new LambdaQueryWrapper<StickerSizeGroup>()
                .eq(StickerSizeGroup::getStatus, 1)
                .eq(brandId != null && !brandId.isBlank(), StickerSizeGroup::getBrandId, brandId)
                .eq(kindId != null && !kindId.isBlank(), StickerSizeGroup::getKindId, kindId)
                .orderByAsc(StickerSizeGroup::getSort)
                .orderByAsc(StickerSizeGroup::getGroupName);
        return groupMapper.selectList(wrapper);
    }

    /**
     * 查某组下的尺码明细(供明细行下拉,仅启用组)
     */
    public List<StickerSize> listSizesByGroupId(String groupId) {
        return sizeMapper.selectList(new LambdaQueryWrapper<StickerSize>()
                .eq(StickerSize::getGroupId, groupId)
                .orderByAsc(StickerSize::getSort)
                .orderByAsc(StickerSize::getSizeName));
    }

    /**
     * 详情(含尺码明细)
     */
    public StickerSizeGroup getById(String id) {
        StickerSizeGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("尺码组不存在");
        }
        group.setSizes(listSizesByGroupId(id));
        return group;
    }

    /**
     * 新增尺码组(含尺码明细一次性提交)
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(StickerSizeGroup entity) {
        checkGroupCodeUnique(entity.getBrandId(), entity.getKindId(), entity.getGroupCode(), null);
        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        entity.setId(null);
        entity.setStatus(entity.getStatus() == null ? 1 : entity.getStatus());
        entity.setSort(entity.getSort() == null ? 0 : entity.getSort());
        entity.setDeleted(0);
        entity.setCreateBy(currentUser);
        entity.setCreateTime(now);
        entity.setUpdateBy(currentUser);
        entity.setUpdateTime(now);
        groupMapper.insert(entity);
        saveSizes(entity.getId(), entity.getSizes(), now);
    }

    /**
     * 修改尺码组(差量更新尺码明细,保留已有尺码id)
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(String id, StickerSizeGroup entity) {
        StickerSizeGroup existing = groupMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("尺码组不存在");
        }
        checkGroupCodeUnique(entity.getBrandId(), entity.getKindId(), entity.getGroupCode(), id);
        entity.setId(id);
        // 防止前端回传覆盖创建信息
        entity.setCreateBy(null);
        entity.setCreateTime(null);
        entity.setDeleted(null);
        entity.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        entity.setUpdateTime(LocalDateTime.now());
        groupMapper.updateById(entity);
        // 差量更新尺码明细: 保留已有id -> 更新; 无id -> 新增; 前端去掉的 -> 软删
        diffUpdateSizes(id, entity.getSizes(), LocalDateTime.now());
    }

    /**
     * 软删尺码组(级联软删其下尺码)
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        StickerSizeGroup existing = groupMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("尺码组不存在");
        }
        groupMapper.deleteById(id);
        sizeMapper.delete(new LambdaQueryWrapper<StickerSize>().eq(StickerSize::getGroupId, id));
    }

    /**
     * 校验组编码在同(brand_id, kind_id)下唯一
     */
    private void checkGroupCodeUnique(String brandId, String kindId, String groupCode, String excludeId) {
        if (groupCode == null || groupCode.isBlank()) {
            throw new BusinessException("尺码组编码不能为空");
        }
        Long count = groupMapper.selectCount(new LambdaQueryWrapper<StickerSizeGroup>()
                .eq(brandId != null && !brandId.isBlank(), StickerSizeGroup::getBrandId, brandId)
                .eq(kindId != null && !kindId.isBlank(), StickerSizeGroup::getKindId, kindId)
                .eq(StickerSizeGroup::getGroupCode, groupCode)
                .ne(excludeId != null && !excludeId.isBlank(), StickerSizeGroup::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("尺码组编码「" + groupCode + "」在同品牌+类别下已存在");
        }
    }

    /**
     * 批量保存尺码明细(过滤空行,补排序) —— 仅新增时使用
     */
    private void saveSizes(String groupId, List<StickerSize> sizes, LocalDateTime now) {
        if (sizes == null || sizes.isEmpty()) {
            return;
        }
        int sort = 0;
        for (StickerSize s : sizes) {
            if (s.getSizeName() == null || s.getSizeName().isBlank()) {
                continue; // 跳过空行
            }
            s.setId(null);
            s.setGroupId(groupId);
            s.setSort(s.getSort() == null ? sort : s.getSort());
            s.setDeleted(0);
            s.setCreateTime(now);
            sizeMapper.insert(s);
            sort++;
        }
    }

    /**
     * 差量更新尺码明细: 有id -> 更新, 无id -> 新增, 前端删除的 -> 软删
     */
    private void diffUpdateSizes(String groupId, List<StickerSize> incomingSizes, LocalDateTime now) {
        // 获取当前数据库中的尺码列表
        List<StickerSize> existingSizes = listSizesByGroupId(groupId);

        // 收集前端传来的已有id集合
        java.util.Set<String> incomingIds = new java.util.HashSet<>();
        if (incomingSizes != null) {
            for (StickerSize s : incomingSizes) {
                if (s.getId() != null && !s.getId().isBlank()) {
                    incomingIds.add(s.getId());
                }
            }
        }

        // 1. 软删前端已移除的尺码(数据库中有但前端没传的)
        for (StickerSize existing : existingSizes) {
            if (!incomingIds.contains(existing.getId())) {
                sizeMapper.deleteById(existing.getId());
            }
        }

        // 2. 处理前端传来的尺码
        if (incomingSizes == null || incomingSizes.isEmpty()) {
            return;
        }
        int sort = 0;
        for (StickerSize s : incomingSizes) {
            if (s.getSizeName() == null || s.getSizeName().isBlank()) {
                continue; // 跳过空行
            }
            s.setGroupId(groupId);
            s.setSort(s.getSort() == null ? sort : s.getSort());
            if (s.getId() != null && !s.getId().isBlank()) {
                // 已有id -> 更新
                s.setDeleted(null); // 不覆盖 deleted
                sizeMapper.updateById(s);
            } else {
                // 无id -> 新增
                s.setId(null);
                s.setDeleted(0);
                s.setCreateTime(now);
                sizeMapper.insert(s);
            }
            sort++;
        }
    }
}
