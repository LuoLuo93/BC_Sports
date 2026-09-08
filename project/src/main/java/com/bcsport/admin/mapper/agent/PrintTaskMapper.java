package com.bcsport.admin.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.agent.PrintTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {

    /**
     * 批量插入。绕过 MP 主键自动填充，调用方必须先 setId(...)
     */
    void batchInsert(@Param("list") List<PrintTask> list);
}
