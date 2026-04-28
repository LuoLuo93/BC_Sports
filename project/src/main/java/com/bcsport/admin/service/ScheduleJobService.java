package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ScheduleJobDTO;
import com.bcsport.admin.dto.ScheduleJobQueryDTO;
import com.bcsport.admin.entity.ScheduleJob;
import com.bcsport.admin.vo.ScheduleJobVO;

public interface ScheduleJobService extends IService<ScheduleJob> {

    PageResult<ScheduleJobVO> pageJobs(PageQuery pageQuery, ScheduleJobQueryDTO queryDTO);

    ScheduleJobVO getJobVOById(String id);

    boolean addJob(ScheduleJobDTO dto);

    boolean updateJob(ScheduleJobDTO dto);

    boolean deleteJob(String id);

    boolean pauseJob(String id);

    boolean resumeJob(String id);

    void runOnce(String id);

    void refreshAllJobs();
}
