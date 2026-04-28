package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.dto.DeptDTO;
import com.bcsport.admin.dto.DeptQueryDTO;
import com.bcsport.admin.entity.Dept;
import com.bcsport.admin.mapper.DeptMapper;
import com.bcsport.admin.service.DeptService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.DeptVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理Service实现类 */
@Service
public class DeptServiceImpl extends ServiceImpl<DeptMapper, Dept> implements DeptService {

    @Override
    public List<DeptVO> selectDeptTree(DeptQueryDTO deptQueryDTO) {
        LambdaQueryWrapper<Dept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dept::getDeleted, 0);
        if (deptQueryDTO != null) {
            if (StringUtils.hasText(deptQueryDTO.getDeptName())) {
                queryWrapper.like(Dept::getDeptName, deptQueryDTO.getDeptName());
            }
            if (deptQueryDTO.getStatus() != null) {
                queryWrapper.eq(Dept::getStatus, deptQueryDTO.getStatus());
            }
        }
        queryWrapper.orderByAsc(Dept::getSort);
        List<Dept> depts = baseMapper.selectList(queryWrapper);
        List<DeptVO> deptVOs = BeanCopyUtils.copyList(depts, DeptVO.class);
        return buildDeptTree(deptVOs);
    }

    @Override
    public DeptVO getDeptVOById(String id) {
        Dept dept = getById(id);
        return BeanCopyUtils.copy(dept, DeptVO.class);
    }

    @Override
    public boolean addDept(DeptDTO deptDTO) {
        Dept dept = BeanCopyUtils.copy(deptDTO, Dept.class);
        return save(dept);
    }

    @Override
    public boolean updateDept(DeptDTO deptDTO) {
        Dept dept = BeanCopyUtils.copy(deptDTO, Dept.class);
        return updateById(dept);
    }

    @Override
    public List<DeptVO> buildDeptTree(List<DeptVO> deptVOs) {
        List<DeptVO> returnList = new ArrayList<>();
        List<String> tempList = deptVOs.stream().map(DeptVO::getId).collect(Collectors.toList());
        for (DeptVO deptVO : deptVOs) {
            // 如果是顶级节点，遍历该父节点的所有子节点
            if (!tempList.contains(deptVO.getParentId())) {
                recursionFn(deptVOs, deptVO);
                returnList.add(deptVO);
            }
        }
        if (returnList.isEmpty()) {
            returnList = deptVOs;
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<DeptVO> list, DeptVO t) {
        // 得到子节点列表
        List<DeptVO> childList = getChildList(list, t);
        t.setChildren(childList);
        for (DeptVO tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<DeptVO> getChildList(List<DeptVO> list, DeptVO t) {
        List<DeptVO> tlist = new ArrayList<>();
        for (DeptVO n : list) {
            if (StringUtils.hasText(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<DeptVO> list, DeptVO t) {
        return getChildList(list, t).size() > 0;
    }
}
