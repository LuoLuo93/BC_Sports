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
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 贴纸本地尺码组 Service
 */
@Service
public class StickerSizeGroupService {

    @Autowired
    private StickerSizeGroupMapper groupMapper;

    @Autowired
    private StickerSizeMapper sizeMapper;

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

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

    /**
     * Excel 批量导入尺码组(含尺码明细)
     * <p>
     * Excel 格式：前 7 列固定(组编码/组名称/品牌/类别/排序/状态/备注)，
     * 后续每两列一对(尺码编码+尺码名称)，同组编码+品牌+类别的多行自动合并。
     *
     * @return {total, success, fail, errors}
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importFromExcel(MultipartFile file) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        // 1. 构建 品牌/类别 名称→ID 反查表
        Map<String, String> brandNameToId = buildNameToIdMap(bjerpProductMapper.getBrands());
        Map<String, String> kindNameToId = buildNameToIdMap(bjerpProductMapper.getKinds());

        List<Map<String, Object>> rawRows;
        try (ExcelReader reader = ExcelUtil.getReader(file.getInputStream())) {
            rawRows = reader.readAll();
        } catch (Exception e) {
            result.put("total", 0);
            result.put("success", 0);
            result.put("fail", 0);
            result.put("errors", List.of("Excel 解析失败：" + e.getMessage()));
            return result;
        }

        int total = rawRows.size();
        if (total == 0) {
            result.put("total", 0);
            result.put("success", 0);
            result.put("fail", 0);
            result.put("errors", List.of());
            return result;
        }

        // 2. 按行解析并按 (groupCode, brandName, kindName) 合并
        // key = "groupCode|brandName|kindName" → 尺码组基本信息 + 尺码明细列表
        Map<String, ParsedGroup> groupMap = new LinkedHashMap<>();

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2; // 第1行是表头
            Map<String, Object> raw = rawRows.get(i);
            try {
                String groupCode = getCellString(raw, "组编码", "groupCode", "GROUPCODE", "group_code");
                String groupName = getCellString(raw, "组名称", "groupName", "GROUPNAME", "group_name");
                String brandName = getCellString(raw, "品牌", "brandName", "BRANDNAME", "brand_name");
                String kindName = getCellString(raw, "类别", "kindName", "KINDNAME", "kind_name");
                String sortStr = getCellString(raw, "排序", "sort", "SORT");
                String statusStr = getCellString(raw, "状态", "status", "STATUS");
                String remark = getCellString(raw, "备注", "remark", "REMARK");

                if (groupCode == null || groupCode.isBlank()) {
                    errors.add("第" + rowNum + "行：组编码不能为空");
                    continue;
                }
                if (groupName == null || groupName.isBlank()) {
                    errors.add("第" + rowNum + "行：组名称不能为空");
                    continue;
                }
                if (brandName == null || brandName.isBlank()) {
                    errors.add("第" + rowNum + "行：品牌不能为空");
                    continue;
                }
                if (kindName == null || kindName.isBlank()) {
                    errors.add("第" + rowNum + "行：类别不能为空");
                    continue;
                }

                groupCode = groupCode.trim();
                groupName = groupName.trim();
                brandName = brandName.trim();
                kindName = kindName.trim();

                // 用 final 变量捕获，供 lambda 使用
                final String fGroupCode = groupCode;
                final String fGroupName = groupName;
                final String fBrandName = brandName;
                final String fKindName = kindName;

                // 反查 ID
                String brandId = brandNameToId.get(brandName);
                if (brandId == null) {
                    errors.add("第" + rowNum + "行：品牌「" + brandName + "」在 ERP 中未找到");
                    continue;
                }
                String kindId = kindNameToId.get(kindName);
                if (kindId == null) {
                    errors.add("第" + rowNum + "行：类别「" + kindName + "」在 ERP 中未找到");
                    continue;
                }

                int sort = 0;
                if (sortStr != null) {
                    try { sort = Integer.parseInt(sortStr.trim()); } catch (NumberFormatException ignored) {}
                }
                final int fSort = sort;
                int status = 1;
                if (statusStr != null) {
                    String s = statusStr.trim();
                    if ("0".equals(s) || "停用".equals(s) || "禁用".equals(s)) {
                        status = 0;
                    }
                }
                final int fStatus = status;

                String mergeKey = fGroupCode + "|" + fBrandName + "|" + fKindName;
                ParsedGroup pg = groupMap.computeIfAbsent(mergeKey, k -> {
                    ParsedGroup g = new ParsedGroup();
                    g.groupCode = fGroupCode;
                    g.groupName = fGroupName;
                    g.brandId = brandId;
                    g.brandName = fBrandName;
                    g.kindId = kindId;
                    g.kindName = fKindName;
                    g.sort = fSort;
                    g.status = fStatus;
                    g.remark = (remark != null ? remark.trim() : "");
                    g.sizes = new ArrayList<>();
                    return g;
                });

                // 3. 动态读取尺码列：从第 8 列(index 7)开始，每两列一对
                // 用 ExcelReader 的 readAll 返回的是 LinkedHashMap，按列顺序排列
                int colIdx = 0;
                List<String> colValues = new ArrayList<>(raw.values().stream()
                        .map(v -> v == null ? "" : v.toString().trim())
                        .collect(Collectors.toList()));

                // 跳过前 7 列，然后每 2 列取一对尺码
                for (int si = 7; si + 1 < colValues.size(); si += 2) {
                    String sizeCode = colValues.get(si);
                    String sizeName = colValues.get(si + 1);
                    if (sizeName != null && !sizeName.isBlank()) {
                        StickerSize sz = new StickerSize();
                        sz.setSizeCode(sizeCode);
                        sz.setSizeName(sizeName.trim());
                        sz.setSort(pg.sizes.size());
                        pg.sizes.add(sz);
                    }
                }
            } catch (Exception e) {
                errors.add("第" + rowNum + "行：" + e.getMessage());
            }
        }

        // 4. 批量写入（upsert）
        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        int success = 0;

        for (ParsedGroup pg : groupMap.values()) {
            try {
                // 查重：同 brandId + kindId + groupCode
                StickerSizeGroup existing = groupMapper.selectOne(
                        new LambdaQueryWrapper<StickerSizeGroup>()
                                .eq(StickerSizeGroup::getBrandId, pg.brandId)
                                .eq(StickerSizeGroup::getKindId, pg.kindId)
                                .eq(StickerSizeGroup::getGroupCode, pg.groupCode)
                                .last("FETCH FIRST 1 ROWS ONLY"));

                if (existing != null) {
                    // 更新基本信息
                    existing.setGroupName(pg.groupName);
                    existing.setSort(pg.sort);
                    existing.setStatus(pg.status);
                    existing.setRemark(pg.remark);
                    existing.setUpdateBy(currentUser);
                    existing.setUpdateTime(now);
                    groupMapper.updateById(existing);
                    // 差量更新尺码明细
                    diffUpdateSizes(existing.getId(), pg.sizes, now);
                } else {
                    // 新增
                    StickerSizeGroup entity = new StickerSizeGroup();
                    entity.setGroupCode(pg.groupCode);
                    entity.setGroupName(pg.groupName);
                    entity.setBrandId(pg.brandId);
                    entity.setBrandName(pg.brandName);
                    entity.setKindId(pg.kindId);
                    entity.setKindName(pg.kindName);
                    entity.setStatus(pg.status);
                    entity.setSort(pg.sort);
                    entity.setRemark(pg.remark);
                    entity.setDeleted(0);
                    entity.setCreateBy(currentUser);
                    entity.setCreateTime(now);
                    entity.setUpdateBy(currentUser);
                    entity.setUpdateTime(now);
                    groupMapper.insert(entity);
                    // 保存尺码明细
                    for (StickerSize sz : pg.sizes) {
                        sz.setGroupId(entity.getId());
                        sz.setDeleted(0);
                        sz.setCreateTime(now);
                        sizeMapper.insert(sz);
                    }
                }
                success++;
            } catch (Exception e) {
                errors.add("尺码组「" + pg.groupCode + "」：" + e.getMessage());
            }
        }

        int fail = total - success;
        // 限制错误列表长度
        if (errors.size() > 100) {
            errors = new ArrayList<>(errors.subList(0, 100));
            errors.add("...共 " + fail + " 条错误，仅显示前 100 条");
        }

        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }

    /** 解析过程中的临时数据结构 */
    private static class ParsedGroup {
        String groupCode, groupName, brandId, brandName, kindId, kindName;
        int sort, status;
        String remark;
        List<StickerSize> sizes;
    }

    /**
     * 多别名兼容取单元格值
     */
    private static String getCellString(Map<String, Object> row, String... keys) {
        if (row == null) return null;
        for (String key : keys) {
            Object val = row.get(key);
            if (val != null) {
                String s = val.toString().trim();
                if (!s.isEmpty()) return s;
            }
        }
        return null;
    }

    /**
     * ERP getBrands/getKinds 返回的 [{ID, ATTRIBNAME}] 构建成 名称→ID 的 Map
     */
    private Map<String, String> buildNameToIdMap(List<Map<String, Object>> rows) {
        Map<String, String> map = new HashMap<>();
        if (rows == null) return map;
        for (Map<String, Object> r : rows) {
            Object id = r.get("ID");
            Object name = r.get("ATTRIBNAME");
            if (id != null && name != null) {
                map.put(name.toString(), id.toString());
            }
        }
        return map;
    }
}
