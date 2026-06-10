package com.bcsport.admin.service.sticker;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.entity.sticker.StickerTemplate;
import com.bcsport.admin.mapper.sticker.StickerTemplateMapper;
import com.bcsport.admin.util.ShiroSecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class StickerTemplateService {

    @Autowired
    private StickerTemplateMapper templateMapper;

    /**
     * 查询模板列表
     */
    public List<StickerTemplate> listTemplates(String templateName) {
        LambdaQueryWrapper<StickerTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateName != null && !templateName.isEmpty()) {
            String escaped = templateName.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.like(StickerTemplate::getTemplateName, escaped);
        }
        wrapper.eq(StickerTemplate::getDeleted, 0)
               .orderByDesc(StickerTemplate::getCreateTime);
        return templateMapper.selectList(wrapper);
    }

    /**
     * 模板分页查询
     */
    public PageResult<StickerTemplate> pageTemplates(PageQuery pageQuery, String templateName) {
        Page<StickerTemplate> page = pageQuery.toPage();
        LambdaQueryWrapper<StickerTemplate> wrapper = new LambdaQueryWrapper<>();
        if (templateName != null && !templateName.isEmpty()) {
            String escaped = templateName.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.like(StickerTemplate::getTemplateName, escaped);
        }
        wrapper.eq(StickerTemplate::getDeleted, 0)
               .orderByDesc(StickerTemplate::getCreateTime);
        Page<StickerTemplate> result = templateMapper.selectPage(page, wrapper);
        return new PageResult<>(result);
    }

    /**
     * 根据ID获取模板
     */
    public StickerTemplate getTemplate(String id) {
        StickerTemplate template = templateMapper.selectById(id);
        if (template == null || template.getDeleted() == 1) {
            throw new BusinessException("模板不存在");
        }
        return template;
    }

    /**
     * 创建模板
     */
    @Transactional(rollbackFor = Exception.class)
    public StickerTemplate createTemplate(StickerTemplate template) {
        template.setId(IdUtil.fastSimpleUUID());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreateBy(ShiroSecurityUtils.getCurrentUsername());
        template.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        template.setDeleted(0);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        if (template.getDpi() == null) {
            template.setDpi(203);
        }
        templateMapper.insert(template);
        return template;
    }

    /**
     * 更新模板
     */
    @Transactional(rollbackFor = Exception.class)
    public StickerTemplate updateTemplate(String id, StickerTemplate template) {
        StickerTemplate existing = getTemplate(id);
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        template.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        template.setCreateTime(existing.getCreateTime());
        template.setCreateBy(existing.getCreateBy());
        template.setDeleted(existing.getDeleted());
        templateMapper.updateById(template);
        return template;
    }

    /**
     * 删除模板（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(String id) {
        StickerTemplate template = getTemplate(id);
        template.setDeleted(1);
        template.setUpdateTime(LocalDateTime.now());
        template.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        templateMapper.updateById(template);
    }

    /**
     * 设为默认模板（同时取消其他默认）
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(String id) {
        getTemplate(id); // 校验存在
        // 取消当前所有默认
        LambdaUpdateWrapper<StickerTemplate> clearDefault = new LambdaUpdateWrapper<>();
        clearDefault.eq(StickerTemplate::getDeleted, 0)
                     .eq(StickerTemplate::getIsDefault, 1)
                     .set(StickerTemplate::getIsDefault, 0)
                     .set(StickerTemplate::getUpdateTime, LocalDateTime.now())
                     .set(StickerTemplate::getUpdateBy, ShiroSecurityUtils.getCurrentUsername());
        templateMapper.update(null, clearDefault);
        // 设为默认
        StickerTemplate tpl = new StickerTemplate();
        tpl.setId(id);
        tpl.setIsDefault(1);
        tpl.setUpdateTime(LocalDateTime.now());
        tpl.setUpdateBy(ShiroSecurityUtils.getCurrentUsername());
        templateMapper.updateById(tpl);
    }

    /**
     * 获取默认模板
     */
    public StickerTemplate getDefaultTemplate() {
        LambdaQueryWrapper<StickerTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StickerTemplate::getDeleted, 0)
               .eq(StickerTemplate::getIsDefault, 1)
               .eq(StickerTemplate::getStatus, 1)
               .last("FETCH FIRST 1 ROWS ONLY");
        return templateMapper.selectOne(wrapper);
    }
}
