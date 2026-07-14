package com.bcsport.admin.service.sticker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.mapper.sticker.BrandTemplateMatchMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BrandTemplateMatchService {

    @Autowired
    private BrandTemplateMatchMapper mapper;

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    public PageResult<BrandTemplateMatch> page(PageQuery pageQuery, String brandName, String kindName) {
        LambdaQueryWrapper<BrandTemplateMatch> wrapper = new LambdaQueryWrapper<BrandTemplateMatch>()
                .orderByDesc(BrandTemplateMatch::getCreateTime);
        if (brandName != null && !brandName.isBlank()) {
            wrapper.like(BrandTemplateMatch::getBrandName, brandName);
        }
        if (kindName != null && !kindName.isBlank()) {
            wrapper.like(BrandTemplateMatch::getKindName, kindName);
        }
        Page<BrandTemplateMatch> page = mapper.selectPage(pageQuery.toPage(), wrapper);
        PageResult<BrandTemplateMatch> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    public List<BrandTemplateMatch> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<BrandTemplateMatch>()
                .eq(BrandTemplateMatch::getIsActive, 1)
                .orderByAsc(BrandTemplateMatch::getBrandName));
    }

    /**
     * 获取去重的模板名称列表
     */
    public List<String> getDistinctTemplateNames() {
        List<BrandTemplateMatch> list = mapper.selectList(new LambdaQueryWrapper<BrandTemplateMatch>()
                .select(BrandTemplateMatch::getTemplateName)
                .eq(BrandTemplateMatch::getIsActive, 1)
                .groupBy(BrandTemplateMatch::getTemplateName)
                .orderByAsc(BrandTemplateMatch::getTemplateName));
        return list.stream()
                .map(BrandTemplateMatch::getTemplateName)
                .filter(name -> name != null && !name.isBlank())
                .collect(java.util.stream.Collectors.toList());
    }

    public BrandTemplateMatch getById(String id) {
        return mapper.selectById(id);
    }

    public BrandTemplateMatch match(String brandId, String kindId) {
        List<BrandTemplateMatch> list = mapper.selectList(new LambdaQueryWrapper<BrandTemplateMatch>()
                .eq(BrandTemplateMatch::getBrandId, brandId)
                .eq(BrandTemplateMatch::getKindId, kindId)
                .eq(BrandTemplateMatch::getIsActive, 1)
                .last("FETCH FIRST 1 ROWS ONLY"));
        return list.isEmpty() ? null : list.get(0);
    }

    public BrandTemplateMatch matchByName(String brandName, String kindName) {
        List<BrandTemplateMatch> list = mapper.selectList(new LambdaQueryWrapper<BrandTemplateMatch>()
                .eq(BrandTemplateMatch::getBrandName, brandName)
                .eq(BrandTemplateMatch::getKindName, kindName)
                .eq(BrandTemplateMatch::getIsActive, 1)
                .last("FETCH FIRST 1 ROWS ONLY"));
        return list.isEmpty() ? null : list.get(0);
    }

    public void create(BrandTemplateMatch entity) {
        entity.setId(null);
        entity.setIsActive(1);
        entity.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateBy(entity.getCreateBy());
        entity.setUpdateTime(entity.getCreateTime());
        mapper.insert(entity);
    }

    public void update(String id, BrandTemplateMatch entity) {
        entity.setId(id);
        entity.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    public void delete(String id) {
        mapper.deleteById(id);
    }

    /**
     * 批量导入（upsert）：以「品牌名称+类别名称」为业务键。
     * Excel 行只含名称，这里反查 ERP 的 M_DIM 表填充 brand_id/kind_id。
     * 命中已有记录则更新（覆盖模板/打印机/备注/状态），否则新增。
     *
     * @param list   待导入的记录（brandName/kindName/templateName/printerName/remark/isActive 已填）
     * @param errors 失败原因收集（行号从1开始计数对应入参顺序）
     * @return 成功写入行数
     */
    public int importBatch(List<BrandTemplateMatch> list, List<String> errors) {
        // 构建名称→ID 反查表（ERP 的 M_DIM 表）
        Map<String, String> brandNameToId = buildNameToIdMap(bjerpProductMapper.getBrands());
        Map<String, String> kindNameToId = buildNameToIdMap(bjerpProductMapper.getKinds());

        String currentUser = ShiroSecurityUtils.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        int success = 0;

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 1;
            BrandTemplateMatch row = list.get(i);
            try {
                String brandId = brandNameToId.get(row.getBrandName());
                if (brandId == null) {
                    errors.add("第" + rowNum + "行：品牌「" + row.getBrandName() + "」在 ERP 中未找到");
                    continue;
                }
                String kindId = kindNameToId.get(row.getKindName());
                if (kindId == null) {
                    errors.add("第" + rowNum + "行：类别「" + row.getKindName() + "」在 ERP 中未找到");
                    continue;
                }

                BrandTemplateMatch existing = mapper.selectByNames(row.getBrandName(), row.getKindName());
                if (existing != null) {
                    // 更新：保留原 id，覆盖业务字段
                    existing.setBrandId(brandId);
                    existing.setKindId(kindId);
                    existing.setTemplateName(row.getTemplateName());
                    existing.setPrinterName(row.getPrinterName());
                    existing.setRemark(row.getRemark());
                    existing.setIsActive(row.getIsActive() == null ? 1 : row.getIsActive());
                    existing.setUpdateBy(currentUser);
                    existing.setUpdateTime(now);
                    mapper.updateById(existing);
                } else {
                    // 新增
                    row.setId(null);
                    row.setBrandId(brandId);
                    row.setKindId(kindId);
                    row.setIsActive(row.getIsActive() == null ? 1 : row.getIsActive());
                    row.setCreateBy(currentUser);
                    row.setCreateTime(now);
                    row.setUpdateBy(currentUser);
                    row.setUpdateTime(now);
                    mapper.insert(row);
                }
                success++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "行：" + e.getMessage());
            }
        }
        return success;
    }

    /**
     * 把 ERP getBrands/getKinds 返回的 [{ID, ATTRIBNAME}] 构建成 名称→ID 的 Map
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
