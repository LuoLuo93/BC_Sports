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
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.ExecutorType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

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

        // 排序：前端点击表头排序时用传入字段，否则用默认排序
        if (pageQuery.getOrderBy() != null && !pageQuery.getOrderBy().trim().isEmpty()) {
            if ("desc".equalsIgnoreCase(pageQuery.getOrderDirection())) {
                queryWrapper.orderByDesc(pageQuery.getOrderBy());
            } else {
                queryWrapper.orderByAsc(pageQuery.getOrderBy());
            }
        } else {
            queryWrapper.orderByAsc("entity_type").orderByAsc("external_id").orderByAsc("entity_name").orderByDesc("update_time");
        }

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
            entity.setId(generateId());
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

    @Override
    public List<EntityChannelVO> listByEntity(String externalId, String entityType) {
        QueryWrapper<EntityChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.eq("external_id", externalId);
        wrapper.eq("entity_type", entityType);
        wrapper.orderByAsc("entity_type").orderByAsc("external_id").orderByAsc("entity_name").orderByDesc("update_time");
        List<EntityChannel> list = entityChannelMapper.selectList(wrapper);
        return convertToVOBatch(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(String externalId, String entityType, List<EntityChannelDTO> list) {
        // 空集合保护：batchSave 是全量替换语义，空集会把该实体所有配置软删，直接拒绝
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("没有可保存的配置");
        }

        // 批次内组合去重：编辑页已允许新增多行，拦截维度完全相同的重复配置
        // (externalId/entityType 全批相同，仅比对 7 个维度字段；与 isDuplicate 字段组合一致)
        Set<String> seenKeys = new HashSet<>();
        for (EntityChannelDTO dto : list) {
            String key = String.join("|",
                    nullSafe(dto.getBrandId()), nullSafe(dto.getChannelTypeId()),
                    nullSafe(dto.getChannelDefId()), nullSafe(dto.getChannelNatureId()),
                    nullSafe(dto.getBusinessTypeId()), nullSafe(dto.getRegionLevel1Id()),
                    nullSafe(dto.getRegionLevel2Id()));
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("存在维度完全相同的重复配置，请去重后再保存");
            }
        }

        // 1. 查询现有记录
        QueryWrapper<EntityChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);
        wrapper.eq("external_id", externalId);
        wrapper.eq("entity_type", entityType);
        List<EntityChannel> existing = entityChannelMapper.selectList(wrapper);

        // 收集提交的已有ID
        Set<String> submittedIds = new HashSet<>();

        for (EntityChannelDTO dto : list) {
            if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
                // 有ID → 更新已有记录
                submittedIds.add(dto.getId());
                EntityChannel entity = new EntityChannel();
                BeanUtils.copyProperties(dto, entity);
                entity.setId(dto.getId());
                entityChannelMapper.updateById(entity);
            } else {
                // 无ID → 新增记录
                EntityChannel entity = new EntityChannel();
                BeanUtils.copyProperties(dto, entity);
                // 使用DTO自身的externalId和entityType，而非参数
                if (entity.getExternalId() == null || entity.getExternalId().isEmpty()) {
                    entity.setExternalId(externalId);
                }
                if (entity.getEntityType() == null || entity.getEntityType().isEmpty()) {
                    entity.setEntityType(entityType);
                }
                entity.setId(generateId());
                entityChannelMapper.insert(entity);
            }
        }

        // 2. 删除未提交的旧记录（用户在前端移除的行）
        for (EntityChannel old : existing) {
            if (!submittedIds.contains(old.getId())) {
                LambdaUpdateWrapper<EntityChannel> delWrapper = new LambdaUpdateWrapper<>();
                delWrapper.eq(EntityChannel::getId, old.getId()).set(EntityChannel::getDeleted, 1);
                entityChannelMapper.update(delWrapper);
            }
        }

        return true;
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
            case "store": return "店仓";
            case "shop": return "店铺";   // 兼容存量数据，新增统一为 store
            case "stock": return "仓库";   // 兼容存量数据
            case "customer": return "客户";
            default: return entityType;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        // ========== 1. 预加载所有名称→ID映射（一次查表） ==========
        Map<String, String> brandNameMap = buildNameToIdMap(brandMapper.selectList(
                new QueryWrapper<Brand>().eq("deleted", 0)), Brand::getBrandName, Brand::getId);

        List<Region> allRegions = regionMapper.selectList(new QueryWrapper<Region>().eq("deleted", 0));
        Map<String, String> regionNameMap = allRegions.stream()
                .collect(Collectors.toMap(Region::getRegionName, Region::getId, (a, b) -> a));
        Map<String, Map<String, String>> regionChildMap = new HashMap<>();
        for (Region r : allRegions) {
            if (r.getParentId() != null && !r.getParentId().isEmpty()) {
                regionChildMap.computeIfAbsent(r.getParentId(), k -> new HashMap<>())
                        .put(r.getRegionName(), r.getId());
            }
        }

        List<ChannelType> allChannelTypes = channelTypeMapper.selectList(new QueryWrapper<ChannelType>().eq("deleted", 0));
        Map<String, String> ctNameMap = allChannelTypes.stream()
                .collect(Collectors.toMap(ChannelType::getTypeName, ChannelType::getId, (a, b) -> a));
        Map<String, Map<String, String>> ctChildMap = new HashMap<>();
        for (ChannelType ct : allChannelTypes) {
            if (ct.getParentId() != null && !ct.getParentId().isEmpty()) {
                ctChildMap.computeIfAbsent(ct.getParentId(), k -> new HashMap<>())
                        .put(ct.getTypeName(), ct.getId());
            }
        }

        List<ChannelNature> allChannelNatures = channelNatureMapper.selectList(new QueryWrapper<ChannelNature>().eq("deleted", 0));
        Map<String, String> cnNameMap = allChannelNatures.stream()
                .collect(Collectors.toMap(ChannelNature::getNatureName, ChannelNature::getId, (a, b) -> a));
        Map<String, Map<String, String>> cnChildMap = new HashMap<>();
        for (ChannelNature cn : allChannelNatures) {
            if (cn.getParentId() != null && !cn.getParentId().isEmpty()) {
                cnChildMap.computeIfAbsent(cn.getParentId(), k -> new HashMap<>())
                        .put(cn.getNatureName(), cn.getId());
            }
        }

        // ========== 2. 预加载已有记录用于去重（一次查表） ==========
        List<EntityChannel> existingList = entityChannelMapper.selectList(
                new QueryWrapper<EntityChannel>().eq("deleted", 0));
        Set<String> existingKeys = new HashSet<>();
        for (EntityChannel ec : existingList) {
            existingKeys.add(buildDuplicateKey(ec));
        }

        // ========== 3. 解析Excel ==========
        ExcelReader reader = ExcelUtil.getReader(file.getInputStream());
        try {
            List<Map<String, Object>> rows = reader.readAll();

            if (rows.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("total", 0);
                result.put("success", 0);
                result.put("fail", 0);
                result.put("errors", Collections.singletonList("Excel中没有数据行，请填写数据后重新上传"));
                return result;
            }

            int maxRows = 50000;
            if (rows.size() > maxRows) {
                Map<String, Object> result = new HashMap<>();
                result.put("total", rows.size());
                result.put("success", 0);
                result.put("fail", rows.size());
                result.put("errors", Collections.singletonList(
                        "数据量过大（" + rows.size() + "行），单次最多导入" + maxRows + "行，请拆分后分批导入"));
                return result;
            }

            int maxErrors = 100;
            List<String> errors = new ArrayList<>();
            List<EntityChannel> toInsert = new ArrayList<>();

            // ========== 4. 逐行解析 + 校验 + 收集待插入实体 ==========
            for (int i = 0; i < rows.size(); i++) {
                int rowNum = i + 2;
                try {
                    Map<String, Object> row = rows.get(i);

                    // 实体类型
                    String entityType = strVal(row.get("实体类型(store/customer)"));
                    if (entityType == null) entityType = strVal(row.get("entityType"));
                    if (entityType == null || entityType.isEmpty()) {
                        String raw = strVal(row.values().iterator().next());
                        if ("店仓".equals(raw) || "store".equalsIgnoreCase(raw)) entityType = "store";
                        else if ("客户".equals(raw) || "customer".equalsIgnoreCase(raw)) entityType = "customer";
                    }
                    if (!"store".equals(entityType) && !"customer".equals(entityType)) {
                        entityType = normalizeEntityType(entityType);
                    }
                    if (entityType == null || entityType.isEmpty()) {
                        if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：实体类型无效，请填 store 或 customer");
                        continue;
                    }

                    // 外部ID
                    String externalId = strVal(row.get("外部ID(ERP编码)"));
                    if (externalId == null) externalId = strVal(row.get("externalId"));
                    if (externalId == null || externalId.isEmpty()) {
                        if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：外部ID不能为空");
                        continue;
                    }

                    // 实体名称
                    String entityName = strVal(row.get("实体名称"));
                    if (entityName == null) entityName = strVal(row.get("entityName"));
                    if (entityName == null || entityName.isEmpty()) entityName = externalId;

                    // 品牌
                    String brandName = strVal(row.get("品牌名称"));
                    if (brandName == null) brandName = strVal(row.get("brandName"));
                    String brandId = null;
                    if (brandName != null && !brandName.isEmpty()) {
                        brandId = brandNameMap.get(brandName);
                        if (brandId == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：品牌「" + brandName + "」未找到");
                            continue;
                        }
                    }

                    // 一级地区
                    String region1 = strVal(row.get("一级地区"));
                    if (region1 == null) region1 = strVal(row.get("regionLevel1Name"));
                    String region1Id = null;
                    if (region1 != null && !region1.isEmpty()) {
                        region1Id = regionNameMap.get(region1);
                        if (region1Id == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：一级地区「" + region1 + "」未找到");
                            continue;
                        }
                    }

                    // 二级地区
                    String region2 = strVal(row.get("二级地区"));
                    if (region2 == null) region2 = strVal(row.get("regionLevel2Name"));
                    String region2Id = null;
                    if (region2 != null && !region2.isEmpty() && region1Id != null) {
                        Map<String, String> children = regionChildMap.get(region1Id);
                        region2Id = children != null ? children.get(region2) : null;
                        if (region2Id == null) {
                            if (errors.size() < maxErrors)
                                errors.add("第" + rowNum + "行：二级地区「" + region2 + "」在「" + region1 + "」下未找到");
                            continue;
                        }
                    }

                    // 渠道类型
                    String ctName = strVal(row.get("渠道类型"));
                    if (ctName == null) ctName = strVal(row.get("channelTypeName"));
                    String ctId = null;
                    if (ctName != null && !ctName.isEmpty()) {
                        ctId = ctNameMap.get(ctName);
                        if (ctId == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：渠道类型「" + ctName + "」未找到");
                            continue;
                        }
                    }

                    // 渠道定义
                    String cdName = strVal(row.get("渠道定义"));
                    if (cdName == null) cdName = strVal(row.get("channelDefName"));
                    String cdId = null;
                    if (cdName != null && !cdName.isEmpty() && ctId != null) {
                        Map<String, String> children = ctChildMap.get(ctId);
                        cdId = children != null ? children.get(cdName) : null;
                        if (cdId == null) {
                            if (errors.size() < maxErrors)
                                errors.add("第" + rowNum + "行：渠道定义「" + cdName + "」在「" + ctName + "」下未找到");
                            continue;
                        }
                    }

                    // 渠道性质
                    String cnName = strVal(row.get("渠道性质"));
                    if (cnName == null) cnName = strVal(row.get("channelNatureName"));
                    String cnId = null;
                    if (cnName != null && !cnName.isEmpty()) {
                        cnId = cnNameMap.get(cnName);
                        if (cnId == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：渠道性质「" + cnName + "」未找到");
                            continue;
                        }
                    }

                    // 销售类型
                    String btName = strVal(row.get("销售类型"));
                    if (btName == null) btName = strVal(row.get("businessTypeName"));
                    String btId = null;
                    if (btName != null && !btName.isEmpty() && cnId != null) {
                        Map<String, String> children = cnChildMap.get(cnId);
                        btId = children != null ? children.get(btName) : null;
                        if (btId == null) {
                            if (errors.size() < maxErrors)
                                errors.add("第" + rowNum + "行：销售类型「" + btName + "」在「" + cnName + "」下未找到");
                            continue;
                        }
                    }

                    // 构建 EntityChannel 实体
                    EntityChannel entity = new EntityChannel();
                    entity.setId(generateId());
                    entity.setEntityType(entityType);
                    entity.setExternalId(externalId);
                    entity.setEntityName(entityName);
                    entity.setBrandId(brandId);
                    entity.setRegionLevel1Id(region1Id);
                    entity.setRegionLevel2Id(region2Id);
                    entity.setChannelTypeId(ctId);
                    entity.setChannelDefId(cdId);
                    entity.setChannelNatureId(cnId);
                    entity.setBusinessTypeId(btId);
                    entity.setStatus(1);
                    entity.setSort(0);
                    entity.setDeleted(0);
                    entity.setCreateTime(new java.util.Date());
                    entity.setUpdateTime(new java.util.Date());

                    // 内存去重：与已有记录 + 本批次已收集的记录比对
                    String dupKey = buildDuplicateKey(entity);
                    if (existingKeys.contains(dupKey)) {
                        if (errors.size() < maxErrors)
                            errors.add("第" + rowNum + "行：该实体渠道配置已存在（重复）");
                        continue;
                    }
                    existingKeys.add(dupKey); // 加入集合，防止本批次内重复
                    toInsert.add(entity);

                } catch (Exception e) {
                    if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：解析异常 - " + e.getMessage());
                }
            }

            // ========== 5. 批量插入（SqlSession BATCH 模式） ==========
            if (!toInsert.isEmpty()) {
                try (SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH, false)) {
                    EntityChannelMapper batchMapper = sqlSession.getMapper(EntityChannelMapper.class);
                    for (int i = 0; i < toInsert.size(); i++) {
                        batchMapper.insert(toInsert.get(i));
                        // 每1000条 flush 一次，避免内存堆积
                        if ((i + 1) % 1000 == 0) {
                            sqlSession.flushStatements();
                        }
                    }
                    sqlSession.flushStatements();
                    sqlSession.commit();
                }
            }

            int failCount = rows.size() - toInsert.size();
            if (failCount > maxErrors && !errors.isEmpty()) {
                errors.add("...共 " + failCount + " 条错误，仅显示前 " + maxErrors + " 条");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", rows.size());
            result.put("success", toInsert.size());
            result.put("fail", failCount);
            result.put("errors", errors);
            return result;
        } finally {
            reader.close();
        }
    }

    /**
     * 统一生成主键：优先 Oracle 序列 SEQ_BC_SPORTS_SYS_ENTITY_CHANNEL，
     * 序列不可用时回退 UUID。单条新增/批量保存/Excel导入三条路径共用，
     * 与项目其它实体 selectNextId 约定一致，避免 ID 生成逻辑散落。
     */
    private String generateId() {
        try {
            return String.valueOf(entityChannelMapper.selectNextId());
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    /**
     * 构建去重key：与 isDuplicate 逻辑一致的字段组合
     */
    private String buildDuplicateKey(EntityChannel ec) {
        return String.join("|",
                nullSafe(ec.getEntityType()),
                nullSafe(ec.getExternalId()),
                nullSafe(ec.getBrandId()),
                nullSafe(ec.getChannelTypeId()),
                nullSafe(ec.getChannelDefId()),
                nullSafe(ec.getChannelNatureId()),
                nullSafe(ec.getBusinessTypeId()),
                nullSafe(ec.getRegionLevel1Id()),
                nullSafe(ec.getRegionLevel2Id()));
    }

    private String nullSafe(String val) {
        return val == null ? "" : val;
    }

    private String strVal(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    private String normalizeEntityType(String raw) {
        if (raw == null) return null;
        switch (raw) {
            case "店仓": case "店铺": case "门店": return "store";
            case "客户": return "customer";
            default: return raw.equalsIgnoreCase("store") ? "store" : (raw.equalsIgnoreCase("customer") ? "customer" : null);
        }
    }

    private <T> Map<String, String> buildNameToIdMap(List<T> list, Function<T, String> nameMapper, Function<T, String> idMapper) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(nameMapper, idMapper, (a, b) -> a));
    }
}
