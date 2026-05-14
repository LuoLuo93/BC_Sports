package com.bcsport.admin.qywxmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.qywx.VxCorpTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VxCorpTagMapper extends BaseMapper<VxCorpTag> {

    void deleteAll();

    void insertBatch(@Param("list") List<VxCorpTag> list);

    List<VxCorpTag> selectAllActive();

    List<VxCorpTag> selectPageGroups(@Param("tagName") String tagName,
                                     @Param("offset") int offset,
                                     @Param("size") int size);

    long selectGroupCount(@Param("tagName") String tagName);

    List<VxCorpTag> selectChildrenByGroupIds(@Param("groupIds") List<String> groupIds);
}
