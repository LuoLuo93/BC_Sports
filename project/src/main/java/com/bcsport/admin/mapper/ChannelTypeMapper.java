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
}
