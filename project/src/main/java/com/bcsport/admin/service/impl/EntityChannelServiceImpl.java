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

        // 优先复活软删记录：避免 deleted=1 的旧行占着唯一键导致 INSERT 报 ORA-00001
        if (reviveIfSoftDeleted(entity)) {
            return true;
        }

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

        // 编辑可能导致 brandId 变更：若目标 (externalId+新brandId+entityType) 存在软删记录，
        // 其仍占着唯一键 uk_external_brand，updateById 改名后会触发 ORA-00001。
        // 处理：物理删除该软删废弃记录（已 deleted=1，属废弃数据），腾出唯一键。
        QueryWrapper<EntityChannel> softDelWrapper = new QueryWrapper<>();
        softDelWrapper.eq("deleted", 1);
        softDelWrapper.eq("entity_type", dto.getEntityType());
        softDelWrapper.eq("external_id", dto.getExternalId());
        addEqOrIsNull(softDelWrapper, "brand_id", dto.getBrandId());
        entityChannelMapper.delete(softDelWrapper);

        EntityChannel entity = new EntityChannel();
        BeanUtils.copyProperties(dto, entity);
        return entityChannelMapper.updateById(entity) > 0;
    }

    /**
     * 检查业务字段组合是否已存在
     * 唯一行 = external_id + brand_id (+ entity_type)，与 DB 唯一约束 uk_external_brand 一致。
     * （旧逻辑按 9 个渠道维度字段组合判重，现已废弃——一店铺+一品牌只允许一行，多渠道维度不再产生多条）
     */
    private boolean isDuplicate(EntityChannelDTO dto, String excludeId) {
        QueryWrapper<EntityChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0);

        wrapper.eq("entity_type", dto.getEntityType());
        wrapper.eq("external_id", dto.getExternalId());
        addEqOrIsNull(wrapper, "brand_id", dto.getBrandId());

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

    /**
     * 复活软删记录：逻辑删除 + 物理唯一约束(uk_external_brand)存在天然冲突——
     * 软删(deleted=1)的行仍占着 (external_id, brand_id) 唯一键，导致删除后无法重新添加同店铺同品牌。
     * 新增/导入时，若发现存在同 (external_id, brand_id, store) 的软删记录，则将其"复活"并更新属性，
     * 而非 INSERT 新行，从而绕开唯一约束冲突。
     *
     * @param entity 待写入的实体（externalId/brandId/entityType 必须已赋值）
     * @return true=已复活并更新了一条软删记录；false=无软删记录，调用方应走 INSERT
     */
    private boolean reviveIfSoftDeleted(EntityChannel entity) {
        QueryWrapper<EntityChannel> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 1);
        wrapper.eq("entity_type", entity.getEntityType());
        wrapper.eq("external_id", entity.getExternalId());
        addEqOrIsNull(wrapper, "brand_id", entity.getBrandId());
        wrapper.last("FETCH FIRST 1 ROWS ONLY");   // Oracle 12c+ 行限语法

        EntityChannel softDeleted = entityChannelMapper.selectOne(wrapper);
        if (softDeleted == null) {
            return false;
        }
        // 复用软删记录 id，覆盖属性并复活
        entity.setId(softDeleted.getId());
        entity.setDeleted(0);
        entity.setUpdateTime(new java.util.Date());
        entityChannelMapper.updateById(entity);
        return true;
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

        // 批次内组合去重：唯一行 = external_id + brand_id（batchSave 内 externalId/entityType 固定，
        // 故仅比对 brandId）。一店铺+一品牌只允许一行，与 isDuplicate / DB 唯一约束一致。
        Set<String> seenKeys = new HashSet<>();
        for (EntityChannelDTO dto : list) {
            if (dto.getBrandId() == null || dto.getBrandId().trim().isEmpty()) {
                throw new IllegalArgumentException("品牌不能为空（店铺+品牌为唯一行）");
            }
            String key = nullSafe(dto.getBrandId());
            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException("存在品牌相同的重复配置，请去重后再保存");
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
                // 优先复活软删记录，避免唯一键冲突；复活失败才走 INSERT
                if (!reviveIfSoftDeleted(entity)) {
                    entity.setId(generateId());
                    entityChannelMapper.insert(entity);
                }
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

        // ========== 2. 预加载已有 store 记录用于 upsert（一次查表） ==========
        // 业务已无"客户"概念：导入数据一律按 store 处理，仅与存量 store 记录比对。
        // 存量 customer 记录完全不参与（不查、不更新、不动），匹配不上即新增。
        // 命中维度：external_id + brand_id（与 DB 唯一约束 uk_external_brand 一致）
        //
        // 同时纳入 deleted=1 的软删记录：逻辑删除 + 物理唯一约束存在冲突，
        // 软删行仍占着唯一键，命中时走"复活更新"而非 INSERT，避免 ORA-00001。
        List<EntityChannel> existingList = entityChannelMapper.selectList(
                new QueryWrapper<EntityChannel>().eq("entity_type", "store"));
        Map<String, EntityChannel> existingMap = new HashMap<>();
        for (EntityChannel ec : existingList) {
            existingMap.put(buildUpsertKey(ec.getExternalId(), ec.getBrandId()), ec);
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
            List<EntityChannel> toUpdate = new ArrayList<>();   // upsert 命中的待更新记录

            // ========== 4. 逐行解析 + 校验 + 收集待插入实体 ==========
            for (int i = 0; i < rows.size(); i++) {
                int rowNum = i + 2;
                try {
                    Map<String, Object> row = rows.get(i);

                    // 实体类型：导入一律按 store 处理（业务已无客户概念），不再读取 Excel 中"实体类型"列。
                    // 存量 customer 记录不参与比对池、不更新、不删除。

                    // 外部ID
                    String externalId = getCol(row, "外部ID(ERP编码)", "externalId");
                    if (externalId == null || externalId.isEmpty()) {
                        if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：外部ID不能为空");
                        continue;
                    }

                    // 实体名称
                    String entityName = getCol(row, "实体名称", "entityName");
                    if (entityName == null || entityName.isEmpty()) entityName = externalId;

                    // 品牌 —— 唯一行 = 店铺(externalId)+品牌(brandId)，brandId 必填
                    String brandName = getCol(row, "品牌名称", "brandName");
                    String brandId = null;
                    if (brandName == null || brandName.isEmpty()) {
                        if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：品牌名称不能为空（店铺+品牌为唯一行）");
                        continue;
                    }
                    brandId = brandNameMap.get(brandName);
                    if (brandId == null) {
                        if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：品牌「" + brandName + "」未找到");
                        continue;
                    }

                    // 一级地区
                    String region1 = getCol(row, "一级地区", "regionLevel1Name");
                    String region1Id = null;
                    if (region1 != null && !region1.isEmpty()) {
                        region1Id = regionNameMap.get(region1);
                        if (region1Id == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：一级地区「" + region1 + "」未找到");
                            continue;
                        }
                    }

                    // 二级地区
                    String region2 = getCol(row, "二级地区", "regionLevel2Name");
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
                    String ctName = getCol(row, "渠道类型", "channelTypeName");
                    String ctId = null;
                    if (ctName != null && !ctName.isEmpty()) {
                        ctId = ctNameMap.get(ctName);
                        if (ctId == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：渠道类型「" + ctName + "」未找到");
                            continue;
                        }
                    }

                    // 渠道定义
                    String cdName = getCol(row, "渠道定义", "channelDefName");
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
                    String cnName = getCol(row, "渠道性质", "channelNatureName");
                    String cnId = null;
                    if (cnName != null && !cnName.isEmpty()) {
                        cnId = cnNameMap.get(cnName);
                        if (cnId == null) {
                            if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：渠道性质「" + cnName + "」未找到");
                            continue;
                        }
                    }

                    // 销售类型
                    String btName = getCol(row, "销售类型", "businessTypeName");
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
                    // 导入一律按 store 处理（业务已无客户概念）；customer 历史记录不动
                    entity.setEntityType("store");
                    entity.setExternalId(externalId);
                    entity.setEntityName(entityName);
                    entity.setBrandId(brandId);
                    entity.setRegionLevel1Id(region1Id);
                    entity.setRegionLevel2Id(region2Id);
                    entity.setChannelTypeId(ctId);
                    entity.setChannelDefId(cdId);
                    entity.setChannelNatureId(cnId);
                    entity.setBusinessTypeId(btId);
                    entity.setStatus(null);
                    entity.setSort(null);
                    entity.setDeleted(0);
                    // createTime/status/sort 仅新增时设；更新时置 null 避免 updateById 覆盖原值
                    // （尤其 status：导入不应把用户手动停用的记录强制改回启用）
                    entity.setCreateTime(null);
                    entity.setUpdateTime(new java.util.Date());

                    // upsert 判定：命中(externalId+brandId 的 store 记录) → 收集待更新；否则 → 待新增
                    String upsertKey = buildUpsertKey(externalId, brandId);
                    EntityChannel existed = existingMap.get(upsertKey);
                    if (existed != null) {
                        // 命中：复用已存在记录 id，按新属性更新
                        entity.setId(existed.getId());
                        // 软删记录命中 → 一并复活(deleted 置回 0)，否则唯一键仍被占用
                        if (existed.getDeleted() != null && existed.getDeleted() == 1) {
                            entity.setDeleted(0);
                        } else {
                            // 正常记录更新时不应动 deleted 字段，置 null 让 updateById 跳过
                            entity.setDeleted(null);
                        }
                        toUpdate.add(entity);
                    } else {
                        // 未命中：新增时补默认值；登记到 map 防本批次内 (externalId+brandId) 重复
                        entity.setStatus(1);
                        entity.setSort(0);
                        entity.setCreateTime(new java.util.Date());
                        existingMap.put(upsertKey, entity);
                        toInsert.add(entity);
                    }

                } catch (Exception e) {
                    if (errors.size() < maxErrors) errors.add("第" + rowNum + "行：解析异常 - " + e.getMessage());
                }
            }

            // ========== 5. 批量写入：新增逐条 insert(冲突降级为错误而非整批失败)，更新走 updateById ==========
            // 原方案用 BATCH insert，但任何一条违反唯一约束会导致整批 flushStatements 抛 BatchUpdateException，
            // 已成功的也无法提交，用户体验差。改为逐条 insert：单条冲突只记错误跳过，其余继续。
            for (EntityChannel ins : toInsert) {
                try {
                    entityChannelMapper.insert(ins);
                } catch (org.springframework.dao.DuplicateKeyException dke) {
                    if (errors.size() < maxErrors) {
                        errors.add("店铺「" + ins.getExternalId() + "」+品牌冲突：该组合已存在或与其它行重复（" + dke.getMostSpecificCause().getMessage() + "）");
                    }
                }
            }
            // 统计实际新增成功条数（toInsert 中未抛冲突的）
            int insertedOk = toInsert.size() - (int) errors.stream().filter(e -> e.contains("冲突")).count();
            // 更新：upsert 命中的记录，按 id 更新渠道属性/地区等字段
            for (EntityChannel upd : toUpdate) {
                entityChannelMapper.updateById(upd);
            }

            // fail = 总行数 - 成功新增 - 更新（剩下的都是校验报错跳过的）
            int failCount = rows.size() - insertedOk - toUpdate.size();
            if (failCount > maxErrors && !errors.isEmpty()) {
                errors.add("...共 " + failCount + " 条错误，仅显示前 " + maxErrors + " 条");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", rows.size());
            result.put("success", insertedOk + toUpdate.size());  // 成功 = 新增 + 更新
            result.put("inserted", insertedOk);                        // 实际新增成功条数（排除冲突）
            result.put("updated", toUpdate.size());                     // 更新条数
            result.put("fail", failCount);
            result.put("errors", errors);
            return result;
        } finally {
            reader.close();
        }
    }

    /**
     * 统一生成主键：优先基于表内最大数字 id + 1（不依赖序列，避免序列与表数据不同步导致主键冲突），
     * 异常时回退 UUID。单条新增/批量保存/Excel导入三条路径共用。
     */
    private String generateId() {
        try {
            return String.valueOf(entityChannelMapper.selectMaxId());
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        }
    }

    /**
     * 构建 upsert 命中 key：external_id + brand_id
     * 与 DB 唯一约束 uk_external_brand 维度一致，用于导入时判定"新增 vs 更新"。
     */
    private String buildUpsertKey(String externalId, String brandId) {
        return nullSafe(externalId) + "|" + nullSafe(brandId);
    }

    private String nullSafe(String val) {
        return val == null ? "" : val;
    }

    private String strVal(Object val) {
        if (val == null) return null;
        String s = String.valueOf(val).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * 从 Excel 行 Map 中按列名取值（兼容表头变体）。
     * 查找顺序：精确匹配主键 → 精确匹配备用键 → 表头以主键开头(如"品牌名称(必填)"匹配"品牌名称")。
     * 解决模板表头加"(必填)"等提示后缀导致 row.get("品牌名称") 取不到值的问题。
     */
    private String getCol(Map<String, Object> row, String primary, String fallback) {
        // 1. 精确匹配
        String val = strVal(row.get(primary));
        if (val != null) return val;
        if (fallback != null) {
            val = strVal(row.get(fallback));
            if (val != null) return val;
        }
        // 2. 模糊匹配：表头以 primary 开头（兼容 "品牌名称(必填)" 这类带后缀的表头）
        for (Map.Entry<String, Object> e : row.entrySet()) {
            String header = e.getKey();
            if (header != null && header.startsWith(primary)) {
                String v = strVal(e.getValue());
                if (v != null) return v;
            }
        }
        return null;
    }

    private <T> Map<String, String> buildNameToIdMap(List<T> list, Function<T, String> nameMapper, Function<T, String> idMapper) {
        if (list == null || list.isEmpty()) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(nameMapper, idMapper, (a, b) -> a));
    }
}
