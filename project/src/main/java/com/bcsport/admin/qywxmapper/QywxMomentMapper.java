package com.bcsport.admin.qywxmapper;

import com.bcsport.admin.entity.qywx.QywxMoment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface QywxMomentMapper {

    void deleteYesterdayData();

    void insertBatch(@Param("list") List<QywxMoment> list);

    /**
     * 分页查询某成员的朋友圈
     */
    List<Map<String, Object>> selectPageByCreator(@Param("creator") String creator,
                                                   @Param("offset") int offset,
                                                   @Param("pageSize") int pageSize);

    /**
     * 查询某成员的朋友圈总数
     */
    long selectCountByCreator(@Param("creator") String creator);

    /**
     * 分页查询所有朋友圈（关联成员详情表获取发布者姓名）
     */
    List<Map<String, Object>> selectPage(@Param("creatorName") String creatorName,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    /**
     * 查询所有朋友圈总数
     */
    long selectCount(@Param("creatorName") String creatorName);
}
