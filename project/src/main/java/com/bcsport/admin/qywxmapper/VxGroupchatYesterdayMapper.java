package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxGroupchatYesterday;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VxGroupchatYesterdayMapper {

    void deleteAll();

    void insertBatch(List<VxGroupchatYesterday> list);

}
