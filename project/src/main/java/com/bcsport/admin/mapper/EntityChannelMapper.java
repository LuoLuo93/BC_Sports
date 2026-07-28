package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.EntityChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 实体渠道配置Mapper
 */
@Mapper
public interface EntityChannelMapper extends BaseMapper<EntityChannel> {

    /**
     * 获取下一个ID（用于Oracle序列)
     */
    @Select("SELECT SEQ_BC_SPORTS_SYS_ENTITY_CHANNEL.NEXTVAL FROM DUAL")
    Long selectNextId();

    /**
     * 基于表内现有最大数字 id 生成下一个 id（不依赖序列，避免序列与表数据不同步导致主键冲突）。
     */
    @Select("SELECT NVL(MAX(TO_NUMBER(id)), 0) + 1 FROM bc_sports_sys_entity_channel WHERE REGEXP_LIKE(id, '^\\d+$')")
    Long selectMaxId();
}
