package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.VxGroupchatYesterday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface VxGroupchatYesterdayMapper {

    void deleteAll();

    void deleteByDate(@Param("dateStr") String dateStr);

    void insertBatch(List<VxGroupchatYesterday> list);

    /**
     * 分页查询某成员的群聊统计
     */
    List<Map<String, Object>> selectPageByOwner(@Param("owner") String owner,
                                                 @Param("offset") int offset,
                                                 @Param("pageSize") int pageSize);

    /**
     * 查询某成员的群聊统计总数
     */
    long selectCountByOwner(@Param("owner") String owner);

}
