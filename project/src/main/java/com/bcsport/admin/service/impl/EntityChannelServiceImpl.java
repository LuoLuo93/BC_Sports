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

import java.util.List;
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

        List<EntityChannelVO> voList = entityPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

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
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addEntityChannel(EntityChannelDTO dto) {
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
        EntityChannel entity = new EntityChannel();
        BeanUtils.copyProperties(dto, entity);
        return entityChannelMapper.updateById(entity) > 0;
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
     * 转换为VO对象
     */
    private EntityChannelVO convertToVO(EntityChannel entity) {
        EntityChannelVO vo = new EntityChannelVO();
        BeanUtils.copyProperties(entity, vo);

        // 设置实体类型名称
        vo.setEntityTypeName(getEntityTypeName(entity.getEntityType()));

        // 设置品牌名称
        if (entity.getBrandId() != null) {
            vo.setBrandName(getBrandName(entity.getBrandId()));
        }

        // 设置渠道类型名称
        if (entity.getChannelTypeId() != null) {
            vo.setChannelTypeName(getChannelTypeName(entity.getChannelTypeId()));
        }

        // 设置渠道定义名称（注意：渠道定义也在 ChannelType 表中）
        if (entity.getChannelDefId() != null) {
            vo.setChannelDefName(getChannelTypeName(entity.getChannelDefId()));
        }

        // 设置渠道性质名称
        if (entity.getChannelNatureId() != null) {
            vo.setChannelNatureName(getChannelNatureName(entity.getChannelNatureId()));
        }

        // 设置经营类型名称（注意：经营类型也在 ChannelNature 表中）
        if (entity.getBusinessTypeId() != null) {
            vo.setBusinessTypeName(getChannelNatureName(entity.getBusinessTypeId()));
        }

        // 设置一级地区名称
        if (entity.getRegionLevel1Id() != null) {
            vo.setRegionLevel1Name(getRegionName(entity.getRegionLevel1Id()));
        }

        // 设置二级地区名称
        if (entity.getRegionLevel2Id() != null) {
            vo.setRegionLevel2Name(getRegionName(entity.getRegionLevel2Id()));
        }

        // 设置状态名称
        vo.setStatusName(entity.getStatus() == 1 ? "启用" : "禁用");

        return vo;
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

    private String getChannelTypeName(String channelTypeId) {
        ChannelType channelType = channelTypeMapper.selectById(channelTypeId);
        return channelType != null ? channelType.getTypeName() : null;
    }

    private String getChannelNatureName(String channelNatureId) {
        ChannelNature channelNature = channelNatureMapper.selectById(channelNatureId);
        return channelNature != null ? channelNature.getNatureName() : null;
    }

    private String getRegionName(String regionId) {
        Region region = regionMapper.selectById(regionId);
        return region != null ? region.getRegionName() : null;
    }

    private String getBrandName(String brandId) {
        Brand brand = brandMapper.selectById(brandId);
        return brand != null ? brand.getBrandName() : null;
    }
}
