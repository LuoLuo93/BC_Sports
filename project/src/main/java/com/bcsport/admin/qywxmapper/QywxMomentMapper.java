package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxMoment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QywxMomentMapper {

    void deleteYesterdayData();

    void insertBatch(@Param("list") List<QywxMoment> list);

}
