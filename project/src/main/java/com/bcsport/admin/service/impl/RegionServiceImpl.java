package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.RegionDTO;
import com.bcsport.admin.dto.RegionQueryDTO;
import com.bcsport.admin.entity.Region;
import com.bcsport.admin.mapper.RegionMapper;
import com.bcsport.admin.service.RegionService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.RegionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RegionServiceImpl extends ServiceImpl<RegionMapper, Region> implements RegionService {

    @Autowired
    private RegionMapper regionMapper;

    @Override
    public List<RegionVO> listByTree(RegionQueryDTO query) {
        // 如果传入了parentId，则返回该父节点下的直接子节点（用于级联联动）
        if (query != null && StringUtils.hasText(query.getParentId())) {
            LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Region::getParentId, query.getParentId());
            wrapper.eq(Region::getDeleted, 0);
            if (query.getStatus() != null) {
                wrapper.eq(Region::getStatus, query.getStatus());
            }
            wrapper.orderByAsc(Region::getSort);
            List<Region> list = this.list(wrapper);
            return BeanCopyUtils.copyList(list, RegionVO.class);
        }

        // 判断是否有名称/编码搜索条件
        boolean hasNameOrCodeSearch = query != null && 
            (StringUtils.hasText(query.getRegionName()) || StringUtils.hasText(query.getRegionCode()));
        
        // 先查询所有数据构建完整树（支持搜索父节点时带出子节点）
        // 注意：有名称/编码搜索时，不过滤状态，避免子节点因状态不同被提前过滤
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        if (!hasNameOrCodeSearch && query != null && query.getStatus() != null) {
            wrapper.eq(Region::getStatus, query.getStatus());
        }
        wrapper.eq(Region::getDeleted, 0);
        wrapper.orderByAsc(Region::getSort);
        List<Region> list = this.list(wrapper);

        List<RegionVO> voList = BeanCopyUtils.copyList(list, RegionVO.class);

        // Build tree
        List<RegionVO> tree = buildTree(voList);

        // 如果存在搜索条件，在树中过滤（匹配节点及其所有子孙节点）
        if (hasNameOrCodeSearch && query != null) {
            tree = filterTree(tree, query.getRegionName(), query.getRegionCode());
        }

        return tree;
    }

    @Override
    public IPage<RegionVO> listByPage(RegionQueryDTO query) {
        Page<Region> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Region::getDeleted, 0);
        
        // 1. 处理层级联动查询逻辑
        if (StringUtils.hasText(query.getLevel2Id())) {
            // 如果选择了二级地区，则直接查该区域
            wrapper.eq(Region::getId, query.getLevel2Id());
        } else if (StringUtils.hasText(query.getLevel1Id())) {
            // 如果仅选择了一级地区，则查该一级地区及其所有直接子节点
            wrapper.and(w -> w.eq(Region::getId, query.getLevel1Id()).or().eq(Region::getParentId, query.getLevel1Id()));
        } else if (!StringUtils.hasText(query.getRegionName()) && !StringUtils.hasText(query.getRegionCode()) && !StringUtils.hasText(query.getParentId())) {
            // 默认浏览模式：仅显示一级节点
            wrapper.and(w -> w.eq(Region::getParentId, "0").or().isNull(Region::getParentId));
        }
        
        // 2. 处理常规搜索
        if (StringUtils.hasText(query.getRegionName())) {
            wrapper.like(Region::getRegionName, query.getRegionName());
        }
        if (StringUtils.hasText(query.getRegionCode())) {
            wrapper.like(Region::getRegionCode, query.getRegionCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Region::getStatus, query.getStatus());
        }
        // 按父级ID升序（在最前）及排序号升序，确保查询结果父级在上，子级在下
        wrapper.orderByAsc(Region::getParentId).orderByAsc(Region::getSort);

        IPage<Region> result = this.page(page, wrapper);
        List<RegionVO> voList = BeanCopyUtils.copyList(result.getRecords(), RegionVO.class);
        
        // 3. 填充路径信息与其父级名称 (扁平化展示支持)
        if (!voList.isEmpty()) {
            // 批量获取所有父级以便构建 fullPath
            List<String> parentIds = voList.stream()
                .map(RegionVO::getParentId)
                .filter(id -> StringUtils.hasText(id) && !"0".equals(id))
                .distinct()
                .collect(Collectors.toList());
            
            Map<String, String> parentNameMap = new java.util.HashMap<>();
            if (!parentIds.isEmpty()) {
                List<Region> parents = this.listByIds(parentIds);
                parentNameMap = parents.stream().collect(Collectors.toMap(Region::getId, Region::getRegionName));
            }

            // 批量获取是否有子节点的标记
            List<String> currentIds = voList.stream().map(RegionVO::getId).collect(Collectors.toList());
            LambdaQueryWrapper<Region> childrenCheckWrapper = new LambdaQueryWrapper<>();
            childrenCheckWrapper.in(Region::getParentId, currentIds);
            childrenCheckWrapper.eq(Region::getDeleted, 0);
            List<Region> anyChildren = this.list(childrenCheckWrapper);
            java.util.Set<String> hasChildrenParentIds = anyChildren.stream().map(Region::getParentId).collect(Collectors.toSet());

            for (RegionVO vo : voList) {
                // 设置 fullPath / parentName
                if (StringUtils.hasText(vo.getParentId()) && !"0".equals(vo.getParentId())) {
                    String pName = parentNameMap.get(vo.getParentId());
                    vo.setParentName(pName);
                    vo.setFullPath(pName + " > " + vo.getRegionName());
                } else {
                    vo.setFullPath(vo.getRegionName());
                }
                
                // 设置 hasChildren 标识
                vo.setHasChildren(hasChildrenParentIds.contains(vo.getId()));
            }
        }

        IPage<RegionVO> voPage = new Page<>();
        org.springframework.beans.BeanUtils.copyProperties(result, voPage);
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 过滤树：保留匹配节点及其所有子孙节点
     */
    private List<RegionVO> filterTree(List<RegionVO> tree, String regionName, String regionCode) {
        List<RegionVO> result = new ArrayList<>();
        for (RegionVO node : tree) {
            // 检查当前节点是否匹配
            boolean nameMatch = !StringUtils.hasText(regionName) ||
                (node.getRegionName() != null && node.getRegionName().contains(regionName));
            boolean codeMatch = !StringUtils.hasText(regionCode) || 
                (node.getRegionCode() != null && node.getRegionCode().contains(regionCode));
            boolean currentMatch = nameMatch && codeMatch;

            if (currentMatch) {
                // 当前节点匹配，保留该节点及其所有子孙节点（原样带出）
                result.add(node);
            } else {
                // 当前节点不匹配，递归过滤子节点，看子节点中是否有匹配的
                List<RegionVO> filteredChildren = new ArrayList<>();
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    filteredChildren = filterTree(node.getChildren(), regionName, regionCode);
                }
                if (!filteredChildren.isEmpty()) {
                    RegionVO newNode = new RegionVO();
                    org.springframework.beans.BeanUtils.copyProperties(node, newNode);
                    newNode.setChildren(filteredChildren);
                    result.add(newNode);
                }
            }
        }
        return result;
    }

    private List<RegionVO> buildTree(List<RegionVO> list) {
        List<RegionVO> tree = new ArrayList<>();
        Map<String, RegionVO> map = list.stream().collect(Collectors.toMap(RegionVO::getId, r -> r));

        for (RegionVO node : list) {
            if ("0".equals(node.getParentId()) || !StringUtils.hasText(node.getParentId())) {
                tree.add(node);
            } else {
                RegionVO parent = map.get(node.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(node);
                } else {
                    tree.add(node);
                }
            }
        }
        return tree;
    }

    @Override
    public RegionVO getRegionVOById(String id) {
        Region region = getById(id);
        return BeanCopyUtils.copy(region, RegionVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRegion(RegionDTO regionDTO) {
        Region region = BeanCopyUtils.copy(regionDTO, Region.class);
        if (region.getId() == null) {
            region.setId(getNextId().toString());
        }
        // 日期和删除字段由 MybatisPlusAutoFillHandler 自动填充
        save(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegion(RegionDTO regionDTO) {
        Region region = BeanCopyUtils.copy(regionDTO, Region.class);
        // updateTime 由 MybatisPlusAutoFillHandler 自动填充
        updateById(region);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRegion(String id) {
        // 递归删除所有子孙节点
        deleteRegionRecursive(id);
        // 删除当前节点
        removeById(id);
    }

    private void deleteRegionRecursive(String parentId) {
        // 查找所有直接子节点
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Region::getParentId, parentId);
        List<Region> children = this.list(wrapper);

        // 递归删除子节点
        for (Region child : children) {
            deleteRegionRecursive(child.getId());
            removeById(child.getId());
        }
    }

    @Override
    public Long getNextId() {
        return regionMapper.selectNextId();
    }
}
