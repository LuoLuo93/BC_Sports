package com.bcsport.admin.mapper.sticker;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.sticker.BrandTemplateMatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BrandTemplateMatchMapper extends BaseMapper<BrandTemplateMatch> {

    /**
     * 按品牌+类别+模板名精确查一条(导入 upsert 用)。同一品牌+类别可配多个模板,
     * 业务键从「品牌+类别」升级为「品牌+类别+模板」,避免双模板导入时互相覆盖。
     */
    @Select("SELECT * FROM sticker_brand_template_match " +
            "WHERE brand_name = #{brandName} AND kind_name = #{kindName} AND template_name = #{templateName} " +
            "FETCH FIRST 1 ROWS ONLY")
    BrandTemplateMatch selectByNamesAndTemplate(@Param("brandName") String brandName, @Param("kindName") String kindName, @Param("templateName") String templateName);
}
