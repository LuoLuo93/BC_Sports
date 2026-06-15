package com.bcsport.admin.service.sticker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import com.bcsport.admin.mapper.sticker.BrandTemplateMatchMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BrandTemplateMatchService {

    @Autowired
    private BrandTemplateMatchMapper mapper;

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
}
