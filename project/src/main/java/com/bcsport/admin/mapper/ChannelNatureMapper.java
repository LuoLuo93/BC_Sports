package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ChannelNature;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 渠道性质Mapper接口
 */
@Mapper
public interface ChannelNatureMapper extends BaseMapper<ChannelNature> {

    @Select("SELECT bc_sports_seq_channel_nature.NEXTVAL FROM dual")
    Long selectNextId();

    /**
     * 基于表内现有最大数字 id 生成下一个 id（不依赖序列，避免序列与表数据不同步导致主键冲突）。
     */
    @Select("SELECT NVL(MAX(TO_NUMBER(id)), 0) + 1 FROM bc_sports_sys_channel_nature WHERE REGEXP_LIKE(id, '^\\d+$')")
    Long selectMaxId();
}
