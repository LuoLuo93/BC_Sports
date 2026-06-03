package com.bcsport.admin.ihrmapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bcsport.admin.entity.ihr.NxcrmUnbindQueue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 南讯会员解绑队列表 Mapper。
 */
@Mapper
public interface NxcrmUnbindQueueMapper extends BaseMapper<NxcrmUnbindQueue> {

    /**
     * 分页查询待处理记录（status=0），按 id 升序保证 FIFO。
     */
    List<NxcrmUnbindQueue> selectPendingPaged(@Param("offset") int offset,
                                              @Param("limit") int limit);

    /**
     * 单条记录状态回写。
     *
     * @param id          记录ID
     * @param status      新状态（1=完成 / 2=失败）
     * @param retryCount  累计重试次数
     * @param processTime 处理时间
     * @param errorMsg    失败原因（成功时为 null）
     */
    int updateStatusById(@Param("id") Long id,
                         @Param("status") Integer status,
                         @Param("retryCount") Integer retryCount,
                         @Param("processTime") Date processTime,
                         @Param("errorMsg") String errorMsg);
}
