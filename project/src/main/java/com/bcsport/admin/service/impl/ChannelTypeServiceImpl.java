package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.ChannelTypeDTO;
import com.bcsport.admin.dto.ChannelTypeQueryDTO;
import com.bcsport.admin.entity.ChannelType;
import com.bcsport.admin.entity.EntityChannel;
import com.bcsport.admin.mapper.ChannelTypeMapper;
import com.bcsport.admin.mapper.EntityChannelMapper;
import com.bcsport.admin.service.ChannelTypeService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ChannelTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 渠道类型服务实现类
 */
@Service
public class ChannelTypeServiceImpl extends ServiceImpl<ChannelTypeMapper, ChannelType> implements ChannelTypeService {

    @Autowired
    private ChannelTypeMapper channelTypeMapper;

    /**
     * 实体渠道配置主表：渠道维度(类型/定义)在此表冗余存父子两列，
     * 字典树移动节点/删除节点时需联动，否则主表父列滞后或引用悬空。
     */
    @Autowired
    private EntityChannelMapper entityChannelMapper;

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
        // 主键冲突重试：序列偶发不同步时，重新取序列值重试，最多 3 次
        int maxRetry = 3;
        for (int i = 0; i < maxRetry; i++) {
            try {
                save(channelType);
                return;
            } catch (org.springframework.dao.DuplicateKeyException e) {
                if (i == maxRetry - 1) {
                    throw e;
                }
                channelType.setId(getNextId().toString());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChannelType(ChannelTypeDTO channelTypeDTO) {
        // 先取旧记录留档父级：updateById 之后再查就是新值，无法判断是否移动过节点
        ChannelType before = channelTypeDTO.getId() != null ? getById(channelTypeDTO.getId()) : null;

        ChannelType channelType = BeanCopyUtils.copy(channelTypeDTO, ChannelType.class);
        // updateTime 由 MybatisPlusAutoFillHandler 自动填充
        updateById(channelType);

        // 节点移动(上级变化)时联动刷新实体渠道配置父列：
        // entity_channel 冗余存 channel_type_id(父)+channel_def_id(子)两列，字典里把"定义"挪到别的类型下后，
        // 已配置行的 channel_type_id 仍是旧父，列表展示与落库都滞后，这里同步改写，详见 syncEntityChannelParentOnMove
        if (before != null && channelTypeDTO.getParentId() != null) {
            syncEntityChannelParentOnMove(channelTypeDTO.getId(), before.getParentId(), channelTypeDTO.getParentId());
        }
    }

    /**
     * 上级(parentId)变化时，联动刷新实体渠道配置的父列。
     * 规则：channel_def_id = 被移动节点 的未删行，channel_type_id 改写为节点当前父级；移成顶级('0'/空)则置 NULL。
     * dto.parentId 为 null 时 updateById 本就不会改父级，视为未移动，调用方已跳过。
     */
    private void syncEntityChannelParentOnMove(String nodeId, String oldParentRaw, String newParentRaw) {
        String oldParent = normalizeTopParent(oldParentRaw);
        String newParent = normalizeTopParent(newParentRaw);
        if (Objects.equals(oldParent, newParent)) {
            return;
        }
        LambdaUpdateWrapper<EntityChannel> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EntityChannel::getChannelDefId, nodeId)
                .eq(EntityChannel::getDeleted, 0)
                .set(EntityChannel::getChannelTypeId, newParent)
                .set(EntityChannel::getUpdateTime, new Date());
        entityChannelMapper.update(null, wrapper);
    }

    /**
     * 顶级 parentId 归一化：'0'/空白/null 统一为 null，便于比较与置空父列。
     */
    private String normalizeTopParent(String parentId) {
        if (parentId == null || parentId.trim().isEmpty() || "0".equals(parentId.trim())) {
            return null;
        }
        return parentId.trim();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannelType(String id) {
        // 收集整棵子树(含自身)：后端删除会把子孙节点一并逻辑删，引用检查必须覆盖全部待删节点
        List<String> subtreeIds = new ArrayList<>();
        collectSubtreeIds(id, subtreeIds);

        // 被实体渠道配置引用则阻止删除：硬删会让引用悬空(字典无 @TableLogic 过滤，列表仍显示已删节点名)
        Long refCount = entityChannelMapper.selectCount(new LambdaQueryWrapper<EntityChannel>()
                .eq(EntityChannel::getDeleted, 0)
                .and(w -> w.in(EntityChannel::getChannelTypeId, subtreeIds)
                        .or().in(EntityChannel::getChannelDefId, subtreeIds)));
        if (refCount != null && refCount > 0) {
            ChannelType node = getById(id);
            throw new IllegalArgumentException("渠道类型「" + (node != null ? node.getTypeName() : id)
                    + "」及其子节点被 " + refCount + " 条实体渠道配置引用，请先在【BI管理→实体渠道】中处理引用后再删除");
        }

        // 递归删除所有子孙节点
        deleteChannelTypeRecursive(id);
        // 删除当前节点
        removeById(id);
    }

    /**
     * 递归收集节点及其全部子孙的 id（与 deleteChannelTypeRecursive 的遍历口径一致，不过滤 deleted）。
     */
    private void collectSubtreeIds(String parentId, List<String> out) {
        out.add(parentId);
        LambdaQueryWrapper<ChannelType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelType::getParentId, parentId);
        for (ChannelType child : this.list(wrapper)) {
            collectSubtreeIds(child.getId(), out);
        }
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
        // 使用 Oracle 序列（NEXTVAL 原子递增，并发安全，不会拿到重复值）
        return channelTypeMapper.selectNextId();
    }
}
