package com.bcsport.admin.mapper.sticker;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BrandTemplateMatchMapper extends BaseMapper<BrandTemplateMatch> {

    /**
     * 按品牌名称+类别名称精确查一条（不限启用状态，导入时允许覆盖停用记录）
     */
    @Select("SELECT * FROM sticker_brand_template_match " +
            "WHERE brand_name = #{brandName} AND kind_name = #{kindName} " +
            "FETCH FIRST 1 ROWS ONLY")
    BrandTemplateMatch selectByNames(@Param("brandName") String brandName, @Param("kindName") String kindName);
}
