package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.IhrAccessToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IhrAccessTokenMapper extends BaseMapper<IhrAccessToken> {
    void insertBatch(@Param("list") List<IhrAccessToken> list);
    void deleteAll();
}
