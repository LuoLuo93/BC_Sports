package com.bcsport.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.DictData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    @Select("SELECT dict_label FROM bc_sports_sys_dict_data WHERE dict_type = #{dictType} AND dict_value = #{dictValue} AND status = 1 AND deleted = 0")
    String getLabelByValue(String dictType, String dictValue);

    @Select("SELECT dict_value FROM bc_sports_sys_dict_data WHERE dict_type = #{dictType} AND dict_label = #{dictLabel} AND status = 1 AND deleted = 0")
    String getValueByLabel(String dictType, String dictLabel);
}
