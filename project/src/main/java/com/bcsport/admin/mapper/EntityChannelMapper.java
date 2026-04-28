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
}
