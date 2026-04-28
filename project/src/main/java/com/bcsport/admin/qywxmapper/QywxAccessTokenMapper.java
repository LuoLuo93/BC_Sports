package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.QywxAccessToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QywxAccessTokenMapper extends BaseMapper<QywxAccessToken> {
    void insertBatch(@Param("list") List<QywxAccessToken> list);
    void deleteAll();
    String queryLatestToken();
}
