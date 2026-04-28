package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.ChannelTypeDTO;
import com.bcsport.admin.dto.ChannelTypeQueryDTO;
import com.bcsport.admin.entity.ChannelType;
import com.bcsport.admin.mapper.ChannelTypeMapper;
import com.bcsport.admin.service.ChannelTypeService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ChannelTypeVO;
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
 * 渠道类型服务实现类
 */
@Service
public class ChannelTypeServiceImpl extends ServiceImpl<ChannelTypeMapper, ChannelType> implements ChannelTypeService {

    @Autowired
    private ChannelTypeMapper channelTypeMapper;

    @Override
    public List<ChannelTypeVO> listByTree(ChannelTypeQueryDTO query) {
        // 如果传入了parentId，则返回该父节点下的直接子节点（用于级联联动）
        if (query != null && StringUtils.hasText(query.getParentId())) {
            LambdaQueryWrapper<ChannelType> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChannelType::getParentId, query.getParentId());
            wrapper.eq(ChannelType::getDeleted, 0);
            if (query.getStatus() != null) {
                wrapper.eq(ChannelType::getStatus, query.getStatus());
            }
            wrapper.orderByAsc(ChannelType::getSort);
            List<ChannelType> list = this.list(wrapper);
            return BeanCopyUtils.copyList(list, ChannelTypeVO.class);
        }

        // 判断是否有名称/编码搜索条件
        boolean hasNameOrCodeSearch = query != null && 
            (StringUtils.hasText(query.getTypeName()) || StringUtils.hasText(query.getTypeCode()));
        
        // 先查询所有数据构建完整树（支持搜索父节点时带出子节点）
        // 注意：有名称/编码搜索时，不过滤状态，避免子节点因状态不同被提前过滤
        LambdaQueryWrapper<ChannelType> wrapper = new LambdaQueryWrapper<>();
        if (!hasNameOrCodeSearch && query != null && query.getStatus() != null) {
            wrapper.eq(ChannelType::getStatus, query.getStatus());
        }
        wrapper.eq(ChannelType::getDeleted, 0);
        wrapper.orderByAsc(ChannelType::getSort);
        List<ChannelType> list = this.list(wrapper);

        List<ChannelTypeVO> voList = BeanCopyUtils.copyList(list, ChannelTypeVO.class);

        // Build tree
        List<ChannelTypeVO> tree = buildTree(voList);

        // 如果有名称/编码搜索条件，在树中过滤（匹配节点及其所有子孙节点）
        if (hasNameOrCodeSearch) {
            tree = filterTree(tree, query.getTypeName(), query.getTypeCode());
        }

        return tree;
    }

    @Override
    public IPage<ChannelTypeVO> listByPage(ChannelTypeQueryDTO query) {
        Page<ChannelType> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<ChannelType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelType::getDeleted, 0);
        
        // 1. 处理层级联动查询逻辑
        if (StringUtils.hasText(query.getLevel2Id())) {
            wrapper.eq(ChannelType::getId, query.getLevel2Id());
        } else if (StringUtils.hasText(query.getLevel1Id())) {
            wrapper.and(w -> w.eq(ChannelType::getId, query.getLevel1Id()).or().eq(ChannelType::getParentId, query.getLevel1Id()));
        } else if (!StringUtils.hasText(query.getTypeName()) && !StringUtils.hasText(query.getTypeCode()) && !StringUtils.hasText(query.getParentId())) {
            // 默认浏览模式：仅显示一级节点
            wrapper.and(w -> w.eq(ChannelType::getParentId, "0").or().isNull(ChannelType::getParentId));
        }
        
        if (StringUtils.hasText(query.getTypeName())) {
            wrapper.like(ChannelType::getTypeName, query.getTypeName());
        }
        if (StringUtils.hasText(query.getTypeCode())) {
            wrapper.like(ChannelType::getTypeCode, query.getTypeCode());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ChannelType::getStatus, query.getStatus());
        }
        // 按父级ID升序（在最前）及排序号升序，确保父节点在上
        wrapper.orderByAsc(ChannelType::getParentId).orderByAsc(ChannelType::getSort);

        IPage<ChannelType> result = this.page(page, wrapper);
        List<ChannelTypeVO> voList = BeanCopyUtils.copyList(result.getRecords(), ChannelTypeVO.class);
        
        if (!voList.isEmpty()) {
            // 批量获取所有父级以便构建fullPath
            List<String> parentIds = voList.stream()
                .map(ChannelTypeVO::getParentId)
                .filter(id -> StringUtils.hasText(id) && !"0".equals(id))
                .distinct()
                .collect(Collectors.toList());
            
            Map<String, String> parentNameMap = new java.util.HashMap<>();
            if (!parentIds.isEmpty()) {
                List<ChannelType> parents = this.listByIds(parentIds);
                parentNameMap = parents.stream().collect(Collectors.toMap(ChannelType::getId, ChannelType::getTypeName));
            }

            // 批量获取是否有子节点的标记
            List<String> currentIds = voList.stream().map(ChannelTypeVO::getId).collect(Collectors.toList());
            LambdaQueryWrapper<ChannelType> childrenCheckWrapper = new LambdaQueryWrapper<>();
            childrenCheckWrapper.in(ChannelType::getParentId, currentIds);
            childrenCheckWrapper.eq(ChannelType::getDeleted, 0);
            List<ChannelType> anyChildren = this.list(childrenCheckWrapper);
            java.util.Set<String> hasChildrenParentIds = anyChildren.stream().map(ChannelType::getParentId).collect(Collectors.toSet());

            for (ChannelTypeVO vo : voList) {
                if (StringUtils.hasText(vo.getParentId()) && !"0".equals(vo.getParentId())) {
                    String pName = parentNameMap.get(vo.getParentId());
                    vo.setParentName(pName);
                    vo.setFullPath(pName + " > " + vo.getTypeName());
                } else {
                    vo.setFullPath(vo.getTypeName());
                }
                vo.setHasChildren(hasChildrenParentIds.contains(vo.getId()));
            }
        }

        IPage<ChannelTypeVO> voPage = new Page<>();
        org.springframework.beans.BeanUtils.copyProperties(result, voPage);
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 过滤树：保留匹配节点及其所有子孙节点
     */
    private List<ChannelTypeVO> filterTree(List<ChannelTypeVO> tree, String typeName, String typeCode) {
        List<ChannelTypeVO> result = new ArrayList<>();
        for (ChannelTypeVO node : tree) {
            // 检查当前节点是否匹配
            boolean nameMatch = !StringUtils.hasText(typeName) || 
                (node.getTypeName() != null && node.getTypeName().contains(typeName));
            boolean codeMatch = !StringUtils.hasText(typeCode) || 
                (node.getTypeCode() != null && node.getTypeCode().contains(typeCode));
            boolean currentMatch = nameMatch && codeMatch;

            if (currentMatch) {
                // 当前节点匹配，保留该节点及其所有子孙节点（原样带出）
                result.add(node);
            } else {
                // 当前节点不匹配，递归过滤子节点，看子节点中是否有匹配的
                List<ChannelTypeVO> filteredChildren = new ArrayList<>();
                if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                    filteredChildren = filterTree(node.getChildren(), typeName, typeCode);
                }
                if (!filteredChildren.isEmpty()) {
                    ChannelTypeVO newNode = new ChannelTypeVO();
                    org.springframework.beans.BeanUtils.copyProperties(node, newNode);
                    newNode.setChildren(filteredChildren);
                    result.add(newNode);
                }
            }
        }
        return result;
    }

    private List<ChannelTypeVO> buildTree(List<ChannelTypeVO> list) {
        List<ChannelTypeVO> tree = new ArrayList<>();
        Map<String, ChannelTypeVO> map = list.stream().collect(Collectors.toMap(ChannelTypeVO::getId, r -> r));

        for (ChannelTypeVO node : list) {
            if ("0".equals(node.getParentId()) || !StringUtils.hasText(node.getParentId())) {
                tree.add(node);
            } else {
                ChannelTypeVO parent = map.get(node.getParentId());
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
    public ChannelTypeVO getChannelTypeVOById(String id) {
        ChannelType channelType = getById(id);
        return BeanCopyUtils.copy(channelType, ChannelTypeVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addChannelType(ChannelTypeDTO channelTypeDTO) {
        ChannelType channelType = BeanCopyUtils.copy(channelTypeDTO, ChannelType.class);
        if (channelType.getId() == null) {
            channelType.setId(getNextId().toString());
        }
        // 日期和删除字段由 MybatisPlusAutoFillHandler 自动填充
        save(channelType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannelType(ChannelTypeDTO channelTypeDTO) {
        ChannelType channelType = BeanCopyUtils.copy(channelTypeDTO, ChannelType.class);
        // updateTime 由 MybatisPlusAutoFillHandler 自动填充
        updateById(channelType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannelType(String id) {
        // 递归删除所有子孙节点
        deleteChannelTypeRecursive(id);
        // 删除当前节点
        removeById(id);
    }

    private void deleteChannelTypeRecursive(String parentId) {
        // 查找所有直接子节点
        LambdaQueryWrapper<ChannelType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelType::getParentId, parentId);
        List<ChannelType> children = this.list(wrapper);

        // 递归删除子节点
        for (ChannelType child : children) {
            deleteChannelTypeRecursive(child.getId());
            removeById(child.getId());
        }
    }

    @Override
    public Long getNextId() {
        return channelTypeMapper.selectNextId();
    }
}
