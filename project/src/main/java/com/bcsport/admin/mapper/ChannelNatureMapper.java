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
}
