package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.EzrVipInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * EZR 会员数据 Mapper（数据源：BC_SPORTS_IHR / SQL Server）
 */
@Mapper
public interface EzrVipInfoMapper extends BaseMapper<EzrVipInfo> {

    /**
     * 分页查询指定门店下的 EZR 会员信息（已关联 YZMobile 取 openID 作为 nasOuid）
     */
    List<EzrVipInfo> selectEzrVipInfoPaged(@Param("shopId") String shopId,
                                           @Param("offset") int offset,
                                           @Param("limit") int limit);

    /**
     * 查询所有出现过的门店 ID（outSgExclusiveShopId 去重）
     */
    List<String> selectDistinctShopList();
}
