package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ChannelType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 渠道类型Mapper接口
 */
@Mapper
public interface ChannelTypeMapper extends BaseMapper<ChannelType> {

    @Select("SELECT bc_sports_seq_channel_type.NEXTVAL FROM dual")
    Long selectNextId();

    /**
     * 基于表内现有最大数字 id 生成下一个 id（不依赖序列，避免序列与表数据不同步导致主键冲突）。
     */
    @Select("SELECT NVL(MAX(TO_NUMBER(id)), 0) + 1 FROM bc_sports_sys_channel_type WHERE REGEXP_LIKE(id, '^\\d+$')")
    Long selectMaxId();
}
