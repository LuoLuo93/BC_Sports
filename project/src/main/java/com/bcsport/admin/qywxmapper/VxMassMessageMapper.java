package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxMassMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VxMassMessageMapper {

    void deleteYesterdayData();

    void insertBatch(List<VxMassMessage> list);

}
