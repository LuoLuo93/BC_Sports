package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxGroupchatYesterday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VxGroupchatYesterdayMapper {

    void deleteAll();

    void deleteByDate(@Param("dateStr") String dateStr);

    void insertBatch(List<VxGroupchatYesterday> list);

}
