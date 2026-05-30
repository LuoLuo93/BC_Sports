package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.EntityChannelDTO;
import com.bcsport.admin.dto.EntityChannelQueryDTO;
import com.bcsport.admin.entity.EntityChannel;
import com.bcsport.admin.entity.ChannelType;
import com.bcsport.admin.entity.ChannelNature;
import com.bcsport.admin.entity.Region;
import com.bcsport.admin.entity.Brand;
import com.bcsport.admin.mapper.EntityChannelMapper;
import com.bcsport.admin.mapper.ChannelTypeMapper;
import com.bcsport.admin.mapper.ChannelNatureMapper;
import com.bcsport.admin.mapper.RegionMapper;
import com.bcsport.admin.mapper.BrandMapper;
import com.bcsport.admin.service.EntityChannelService;
import com.bcsport.admin.vo.EntityChannelVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 实体渠道配置服务实现类
 */
@Service
public class EntityChannelServiceImpl implements EntityChannelService {

    @Autowired
    private EntityChannelMapper entityChannelMapper;

    @Autowired
    private ChannelTypeMapper channelTypeMapper;

    @Autowired
    private ChannelNatureMapper channelNatureMapper;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Override
    public PageResult<EntityChannelVO> pageEntityChannels(PageQuery pageQuery, EntityChannelQueryDTO queryDTO) {
        Page<EntityChannel> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());

        QueryWrapper<EntityChannel> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);

        if (queryDTO.getEntityType() != null && !queryDTO.getEntityType().trim().isEmpty()) {
            queryWrapper.eq("entity_type", queryDTO.getEntityType());
        }

        if (queryDTO.getEntityName() != null && !queryDTO.getEntityName().trim().isEmpty()) {
            queryWrapper.like("entity_name", queryDTO.getEntityName());
        }

        if (queryDTO.getExternalId() != null && !queryDTO.getExternalId().trim().isEmpty()) {
            queryWrapper.like("external_id", queryDTO.getExternalId());
        }

        if (queryDTO.getBrandId() != null && !queryDTO.getBrandId().trim().isEmpty()) {
            queryWrapper.eq("brand_id", queryDTO.getBrandId());
        }

        if (queryDTO.getChannelTypeId() != null && !queryDTO.getChannelTypeId().trim().isEmpty()) {
            queryWrapper.eq("channel_type_id", queryDTO.getChannelTypeId());
        }

        if (queryDTO.getChannelDefId() != null && !queryDTO.getChannelDefId().trim().isEmpty()) {
            queryWrapper.eq("channel_def_id", queryDTO.getChannelDefId());
        }

        if (queryDTO.getChannelNatureId() != null && !queryDTO.getChannelNatureId().trim().isEmpty()) {
            queryWrapper.eq("channel_nature_id", queryDTO.getChannelNatureId());
        }

        if (queryDTO.getBusinessTypeId() != null && !queryDTO.getBusinessTypeId().trim().isEmpty()) {
            queryWrapper.eq("business_type_id", queryDTO.getBusinessTypeId());
        }

        if (queryDTO.getRegionLevel1Id() != null && !queryDTO.getRegionLevel1Id().trim().isEmpty()) {
            queryWrapper.eq("region_level1_id", queryDTO.getRegionLevel1Id());
        }

        if (queryDTO.getRegionLevel2Id() != null && !queryDTO.getRegionLevel2Id().trim().isEmpty()) {
            queryWrapper.eq("region_level2_id", queryDTO.getRegionLevel2Id());
        }

        if (queryDTO.getStatus() != null) {
            queryWrapper.eq("status", queryDTO.getStatus());
        }

        queryWrapper.orderByDesc("create_time");

        IPage<EntityChannel> entityPage = entityChannelMapper.selectPage(page, queryWrapper);

        List<EntityChannelVO> voList = convertToVOBatch(entityPage.getRecords());

        PageResult<EntityChannelVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(entityPage.getTotal());
        pageResult.setPages(entityPage.getPages());
        pageResult.setPageNum(entityPage.getCurrent());
        pageResult.setPageSize(entityPage.getSize());
        pageResult.setHasPrevious(entityPage.getCurrent() > 1);
        pageResult.setHasNext(entityPage.getCurrent() < entityPage.getPages());
        return pageResult;
    }

    @Override
    public EntityChannelVO getEntityChannelVOById(String id) {
        EntityChannel entity = entityChannelMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            return null;
        }
        return convertToVOBatch(Collections.singletonList(entity)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addEntityChannel(EntityChannelDTO dto) {
        if (isDuplicate(dto, null)) {
            throw new IllegalArgumentException("该实体渠道配置已存在，请勿重复新增");
        }

        EntityChannel entity = new EntityChannel();
        BeanUtils.copyProperties(dto, entity);

        // 生成ID
        if (entity.getId() == null || entity.getId().isEmpty()) {
            try {
                entity.setId(String.valueOf(entityChannelMapper.selectNextId()));
            } catch (Exception e) {
                // 如果序列不存在，使用UUID
                entity.setId(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            }
        }

        return entityChannelMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEntityChannel(EntityChannelDTO dto) {
        if (isDuplicate(dto, dto.getId())) {
            throw new IllegalArgumentException("该实体渠道配置已存在，请勿重复提交");
        }

        EntityChannel entity = new EntityChannel();
        BeanUtils.copyProperties(dto, entity);
        return entityChannelMapper.updateById(entity) > 0;
    }

    /**
     * 检查业务字段组合是否已存在
     */
    private boolean isDuplicate(EntityChannelDTO dto, String excludeId) {
        QueryWrapper<EntityChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);

        wrapper.eq("entity_type", dto.getEntityType());
        wrapper.eq("external_id", dto.getExternalId());

        addEqOrIsNull(wrapper, "brand_id", dto.getBrandId());
        addEqOrIsNull(wrapper, "channel_type_id", dto.getChannelTypeId());
        addEqOrIsNull(wrapper, "channel_def_id", dto.getChannelDefId());
        addEqOrIsNull(wrapper, "channel_nature_id", dto.getChannelNatureId());
        addEqOrIsNull(wrapper, "business_type_id", dto.getBusinessTypeId());
        addEqOrIsNull(wrapper, "region_level1_id", dto.getRegionLevel1Id());
        addEqOrIsNull(wrapper, "region_level2_id", dto.getRegionLevel2Id());

        if (excludeId != null && !excludeId.isEmpty()) {
            wrapper.ne("id", excludeId);
        }

        return entityChannelMapper.selectCount(wrapper) > 0;
    }

    private void addEqOrIsNull(QueryWrapper<EntityChannel> wrapper, String column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            wrapper.eq(column, value);
        } else {
            wrapper.isNull(column);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEntityChannel(String id) {
        // 逻辑删除：直接更新 deleted 字段（该类没有继承 ServiceImpl，直接用 Mapper）
        LambdaUpdateWrapper<EntityChannel> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EntityChannel::getId, id).set(EntityChannel::getDeleted, 1);
        return entityChannelMapper.update(wrapper) > 0;
    }

    /**
     * 批量转换为VO对象（消除N+1查询）
     */
    private List<EntityChannelVO> convertToVOBatch(List<EntityChannel> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> channelTypeIds = new HashSet<>();
        Set<String> channelNatureIds = new HashSet<>();
        Set<String> regionIds = new HashSet<>();
        Set<String> brandIds = new HashSet<>();

        for (EntityChannel e : entities) {
            addIfPresent(channelTypeIds, e.getChannelTypeId());
            addIfPresent(channelTypeIds, e.getChannelDefId());
            addIfPresent(channelNatureIds, e.getChannelNatureId());
            addIfPresent(channelNatureIds, e.getBusinessTypeId());
            addIfPresent(regionIds, e.getRegionLevel1Id());
            addIfPresent(regionIds, e.getRegionLevel2Id());
            addIfPresent(brandIds, e.getBrandId());
        }

        Map<String, String> channelTypeNameMap = channelTypeIds.isEmpty() ? Collections.emptyMap() :
                buildNameMap(channelTypeMapper.selectBatchIds(new ArrayList<>(channelTypeIds)),
                        ChannelType::getId, ChannelType::getTypeName);
        Map<String, String> channelNatureNameMap = channelNatureIds.isEmpty() ? Collections.emptyMap() :
                buildNameMap(channelNatureMapper.selectBatchIds(new ArrayList<>(channelNatureIds)),
                        ChannelNature::getId, ChannelNature::getNatureName);
        Map<String, String> regionNameMap = regionIds.isEmpty() ? Collections.emptyMap() :
                buildNameMap(regionMapper.selectBatchIds(new ArrayList<>(regionIds)),
                        Region::getId, Region::getRegionName);
        Map<String, String> brandNameMap = brandIds.isEmpty() ? Collections.emptyMap() :
                buildNameMap(brandMapper.selectBatchIds(new ArrayList<>(brandIds)),
                        Brand::getId, Brand::getBrandName);

        return entities.stream().map(entity -> {
            EntityChannelVO vo = new EntityChannelVO();
            BeanUtils.copyProperties(entity, vo);
            vo.setEntityTypeName(getEntityTypeName(entity.getEntityType()));

            if (entity.getBrandId() != null) {
                vo.setBrandName(brandNameMap.get(entity.getBrandId()));
            }
            if (entity.getChannelTypeId() != null) {
                vo.setChannelTypeName(channelTypeNameMap.get(entity.getChannelTypeId()));
            }
            if (entity.getChannelDefId() != null) {
                vo.setChannelDefName(channelTypeNameMap.get(entity.getChannelDefId()));
            }
            if (entity.getChannelNatureId() != null) {
                vo.setChannelNatureName(channelNatureNameMap.get(entity.getChannelNatureId()));
            }
            if (entity.getBusinessTypeId() != null) {
                vo.setBusinessTypeName(channelNatureNameMap.get(entity.getBusinessTypeId()));
            }
            if (entity.getRegionLevel1Id() != null) {
                vo.setRegionLevel1Name(regionNameMap.get(entity.getRegionLevel1Id()));
            }
            if (entity.getRegionLevel2Id() != null) {
                vo.setRegionLevel2Name(regionNameMap.get(entity.getRegionLevel2Id()));
            }
            vo.setStatusName(entity.getStatus() == 1 ? "启用" : "禁用");
            return vo;
        }).collect(Collectors.toList());
    }

    private void addIfPresent(Set<String> set, String value) {
        if (value != null && !value.isEmpty()) {
            set.add(value);
        }
    }

    private <T> Map<String, String> buildNameMap(List<T> list, Function<T, String> keyMapper, Function<T, String> valueMapper) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> a));
    }

    private String getEntityTypeName(String entityType) {
        if (entityType == null) return null;
        switch (entityType) {
            case "shop": return "店铺";
            case "stock": return "仓库";
            case "customer": return "客户";
            default: return entityType;
        }
    }
}
