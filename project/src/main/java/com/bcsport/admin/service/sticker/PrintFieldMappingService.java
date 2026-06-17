package com.bcsport.admin.service.sticker;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.entity.sticker.PrintFieldMapping;
import com.bcsport.admin.mapper.sticker.PrintFieldMappingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PrintFieldMappingService {

    @Autowired
    private PrintFieldMappingMapper mapper;

    public Page<PrintFieldMapping> page(int pageNum, int pageSize, String templateId) {
        LambdaQueryWrapper<PrintFieldMapping> wrapper = new LambdaQueryWrapper<>();
        if (templateId != null && !templateId.isBlank()) {
            wrapper.eq(PrintFieldMapping::getTemplateId, templateId);
        }
        wrapper.orderByAsc(PrintFieldMapping::getSortOrder);
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 按模板名称查询字段映射
     * 注意：template_id 字段存储的是模板名称（一个模板名称可能被多个品牌/类别引用）
     */
    public List<PrintFieldMapping> getByTemplateName(String templateName) {
        return mapper.selectList(
            new LambdaQueryWrapper<PrintFieldMapping>()
                .eq(PrintFieldMapping::getTemplateId, templateName)
                .orderByAsc(PrintFieldMapping::getSortOrder)
        );
    }

    public void create(PrintFieldMapping entity) {
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        mapper.insert(entity);
    }

    public void update(String id, PrintFieldMapping entity) {
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        mapper.updateById(entity);
    }

    public void delete(String id) {
        mapper.deleteById(id);
    }

    /**
     * 按模板名称删除所有字段映射
     */
    public void deleteByTemplateName(String templateName) {
        mapper.delete(
            new LambdaQueryWrapper<PrintFieldMapping>()
                .eq(PrintFieldMapping::getTemplateId, templateName)
        );
    }
}
