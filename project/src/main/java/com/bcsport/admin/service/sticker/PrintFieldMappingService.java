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

    public Page<PrintFieldMapping> page(int pageNum, int pageSize) {
        LambdaQueryWrapper<PrintFieldMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PrintFieldMapping::getSortOrder);
        return mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 查询全局唯一的字段映射列表（所有打印模板字段名一致，共用一份配置）
     */
    public List<PrintFieldMapping> getAll() {
        return mapper.selectList(
            new LambdaQueryWrapper<PrintFieldMapping>()
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
}
