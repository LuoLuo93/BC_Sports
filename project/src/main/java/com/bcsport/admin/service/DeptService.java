package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.dto.DeptDTO;
import com.bcsport.admin.dto.DeptQueryDTO;
import com.bcsport.admin.entity.Dept;
import com.bcsport.admin.vo.DeptVO;

import java.util.List;

/**
 * 部门管理Service接口
 */
public interface DeptService extends IService<Dept> {
    
    /**
     * 查询部门名     */
    List<DeptVO> selectDeptTree(DeptQueryDTO deptQueryDTO);
    
    /**
     * 根据ID查询部门VO
     */
    DeptVO getDeptVOById(String id);
    
    /**
     * 新增部门
     */
    boolean addDept(DeptDTO deptDTO);
    
    /**
     * 更新部门
     */
    boolean updateDept(DeptDTO deptDTO);
    
    /**
     * 构建部门名     */
    List<DeptVO> buildDeptTree(List<DeptVO> depts);
}
