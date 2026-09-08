package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.NanXOrderMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NanXOrderMapper extends BaseMapper<NanXOrderMaster> {

    List<NanXOrderMaster> selectAllOrdersPaged(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * keyset 分页：按 outTradeId 游标前进（after 为空取首页），替代 offset 深翻页
     */
    List<NanXOrderMaster> selectOrdersAfter(@Param("after") String after, @Param("limit") int limit);
}
