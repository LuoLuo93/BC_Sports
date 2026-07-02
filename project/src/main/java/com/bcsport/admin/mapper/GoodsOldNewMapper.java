package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.bi.GoodsOldNew;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsOldNewMapper extends BaseMapper<GoodsOldNew> {

    /**
     * 批量 MERGE upsert（品牌+货号+产品季 去重）
     */
    void mergeBatch(@Param("list") List<GoodsOldNew> list);
}
