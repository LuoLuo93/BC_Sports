package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.ChannelNatureDTO;
import com.bcsport.admin.dto.ChannelNatureQueryDTO;
import com.bcsport.admin.entity.ChannelNature;
import com.bcsport.admin.mapper.ChannelNatureMapper;
import com.bcsport.admin.service.ChannelNatureService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ChannelNatureVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 渠道性质服务实现类
 */
@Service
public class ChannelNatureServiceImpl extends ServiceImpl<ChannelNatureMapper, ChannelNature> implements ChannelNatureService {

    @Autowired
    private ChannelNatureMapper channelNatureMapper;

    @Override
    public List<ChannelNatureVO> listByTree(ChannelNatureQueryDTO query) {
        // 如果传入了parentId，则返回该父节点下的直接子节点（用于级联联动）
        if (query != null && StringUtils.hasText(query.getParentId())) {
            LambdaQueryWrapper<ChannelNature> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChannelNature::getParentId, query.getParentId());
            wrapper.eq(ChannelNature::getDeleted, 0);
            if (query.getStatus() != null) {
                wrapper.eq(ChannelNature::getStatus, query.getStatus());
            }
            wrapper.orderByAsc(ChannelNature::getSort);
            List<ChannelNature> list = this.list(wrapper);
            return BeanCopyUtils.copyList(list, ChannelNatureVO.class);
        }

        // 判断是否有名称/编码搜索条件
        boolean hasNameOrCodeSearch = query != null && 
            (StringUtils.hasText(query.getNatureName()) || StringUtils.hasText(query.getNatureCode()));
        
        // 先查询所有数据构建完整树（支持搜索父节点时带出子节点）
        // 注意：有名称/编码搜索时，不过滤状态，避免子节点因状态不同被提前过滤
        LambdaQueryWrapper<ChannelNature> wrapper = new LambdaQueryWrapper<>();
        if (!hasNameOrCodeSearch && query != null && query.getStatus() != null) {
            wrapper.eq(ChannelNature::getStatus, query.getStatus());
        }
        wrapper.eq(ChannelNature::getDeleted, 0);
        wrapper.orderByAsc(ChannelNature::getSort);
        List<ChannelNature> list = this.list(wrapper);

        List<ChannelNatureVO> voList = BeanCopyUtils.copyList(list, ChannelNatureVO.class);

        // Build tree
        List<ChannelNatureVO> tree = buildTree(voList);

        // 如果有名称/编码搜索条件，在树中过滤（匹配节点及其所有子孙节点）
        if (hasNameOrCodeSearch) {
            tree = filterTree(tree, query.getNatureName(), query.getNatureCode());
        }

        return tree;
    }

    @Override
    public IPage<ChannelNatureVO> listByPage(ChannelNatureQueryDTO query) {
        Page<ChannelNature> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<ChannelNature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelNature::getDeleted, 0);
        
        // 1. 处理层级联动查询逻辑
        if (StringUtils.hasText(query.getLevel2Id())) {
            wrapper.eq(ChannelNature::getId, query.getLevel2Id());
        } else if (StringUtils.hasText(query.getLevel1Id())) {
            wrapper.and(w -> w.eq(ChannelNature::getId, query.getLevel1Id()).or().eq(ChannelNature::getParentId, query.getLevel1Id()));
        } else if (!StringUtils.hasText(query.getNatureName()) && !StringUtils.hasText(query.getNatureCode()) && !StringUtils.hasText(query.getParentId())) {
            // 默认浏览模式：仅显示一级节点
            wrapper.and(w -> w.eq(ChannelNature::getParentId, "0").or().isNull(ChannelNature::getParentId));
        }
        
        if (StringUtils.hasText(query.getNatureName())) {
            wrapper.like(ChannelNature::getNatureName, query.getNatureName());
        }
        if (StringUtils.hasText(query.getNatureCode())) {
            wrapper.like(ChannelNature::getNatureCode, query.getNatureCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ChannelNature::getStatus, query.getStatus());
        }
        // 按父级ID升序（在最前）及排序号升序，确保父节点在上
        wrapper.orderByAsc(ChannelNature::getParentId).orderByAsc(ChannelNature::getSort);

        IPage<ChannelNature> result = this.page(page, wrapper);
        List<ChannelNatureVO> voList = BeanCopyUtils.copyList(result.getRecords(), ChannelNatureVO.class);
        
        if (!voList.isEmpty()) {
            // 批量获取所有父级以便构建fullPath
            List<String> parentIds = voList.stream()
                .map(ChannelNatureVO::getParentId)
                .filter(id -> StringUtils.hasText(id) && !"0".equals(id))
                .distinct()
                .collect(Collectors.toList());
            
            Map<String, String> parentNameMap = new java.util.HashMap<>();
            if (!parentIds.isEmpty()) {
                List<ChannelNature> parents = this.listByIds(parentIds);
                parentNameMap = parents.stream().collect(Collectors.toMap(ChannelNature::getId, ChannelNature::getNatureName));
            }

            // 批量获取是否有子节点的标记
            List<String> currentIds = voList.stream().map(ChannelNatureVO::getId).collect(Collectors.toList());
            LambdaQueryWrapper<ChannelNature> childrenCheckWrapper = new LambdaQueryWrapper<>();
            childrenCheckWrapper.in(ChannelNature::getParentId, currentIds);
            childrenCheckWrapper.eq(ChannelNature::getDeleted, 0);
            List<ChannelNature> anyChildren = this.list(childrenCheckWrapper);
            java.util.Set<String> hasChildrenParentIds = anyChildren.stream().map(ChannelNature::getParentId).collect(Collectors.toSet());

            for (ChannelNatureVO vo : voList) {
                if (StringUtils.hasText(vo.getParentId()) && !"0".equals(vo.getParentId())) {
                    String pName = parentNameMap.get(vo.getParentId());
                    vo.setParentName(pName);
                    vo.setFullPath(pName + " > " + vo.getNatureName());
                } else {
                    vo.setFullPath(vo.getNatureName());
                }
                vo.setHasChildren(hasChildrenParentIds.contains(vo.getId()));
            }
        }

        IPage<ChannelNatureVO> voPage = new Page<>();
        org.springframework.beans.BeanUtils.copyProperties(result, voPage);
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 过滤树：保留匹配节点及其所有子孙节点
     */
    private List<ChannelNatureVO> filterTree(List<ChannelNatureVO> tree, String natureName, String natureCode) {
        List<ChannelNatureVO> result = new ArrayList<>();
        for (ChannelNatureVO node : tree) {
            // 检查当前节点是否匹配
            boolean nameMatch = !StringUtils.hasText(natureName) || 
                (node.getNatureName() != null && node.getNatureName().contains(natureName));
            boolean codeMatch = !StringUtils.hasText(natureCode) || 
                (node.getNatureCode() != null && node.getNatureCode().contains(natureCode));
            boolean currentMatch = nameMatch && codeMatch;

            if (currentMatch) {
                // 当前节点匹配，保留该节点及其所有子孙节点（原样带出）
                result.add(node);
            } else {
                // 当前节点不匹配，递归过滤子节点，看子节点中是否有匹配的
                List<ChannelNatureVO> filteredChildren = new ArrayList<>();
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    filteredChildren = filterTree(node.getChildren(), natureName, natureCode);
                }
                if (!filteredChildren.isEmpty()) {
                    ChannelNatureVO newNode = new ChannelNatureVO();
                    org.springframework.beans.BeanUtils.copyProperties(node, newNode);
                    newNode.setChildren(filteredChildren);
                    result.add(newNode);
                }
            }
        }
        return result;
    }

    private List<ChannelNatureVO> buildTree(List<ChannelNatureVO> list) {
        List<ChannelNatureVO> tree = new ArrayList<>();
        Map<String, ChannelNatureVO> map = list.stream().collect(Collectors.toMap(ChannelNatureVO::getId, r -> r));

        for (ChannelNatureVO node : list) {
            if ("0".equals(node.getParentId()) || !StringUtils.hasText(node.getParentId())) {
                tree.add(node);
            } else {
                ChannelNatureVO parent = map.get(node.getParentId());
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
    public ChannelNatureVO getChannelNatureVOById(String id) {
        ChannelNature channelNature = getById(id);
        return BeanCopyUtils.copy(channelNature, ChannelNatureVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addChannelNature(ChannelNatureDTO channelNatureDTO) {
        ChannelNature channelNature = BeanCopyUtils.copy(channelNatureDTO, ChannelNature.class);
        if (channelNature.getId() == null) {
            channelNature.setId(getNextId().toString());
        }
        // 日期和删除字段由 MybatisPlusAutoFillHandler 自动填充
        save(channelNature);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannelNature(ChannelNatureDTO channelNatureDTO) {
        ChannelNature channelNature = BeanCopyUtils.copy(channelNatureDTO, ChannelNature.class);
        // updateTime 由 MybatisPlusAutoFillHandler 自动填充
        updateById(channelNature);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannelNature(String id) {
        // 递归删除所有子孙节点
        deleteChannelNatureRecursive(id);
        // 删除当前节点
        removeById(id);
    }

    private void deleteChannelNatureRecursive(String parentId) {
        // 查找所有直接子节点
        LambdaQueryWrapper<ChannelNature> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelNature::getParentId, parentId);
        List<ChannelNature> children = this.list(wrapper);

        // 递归删除子节点
        for (ChannelNature child : children) {
            deleteChannelNatureRecursive(child.getId());
            removeById(child.getId());
        }
    }

    @Override
    public Long getNextId() {
        return channelNatureMapper.selectNextId();
    }
}
