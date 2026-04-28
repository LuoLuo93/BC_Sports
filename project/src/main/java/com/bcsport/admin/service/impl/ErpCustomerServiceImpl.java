package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.ErpCustomerDTO;
import com.bcsport.admin.dto.ErpCustomerQueryDTO;
import com.bcsport.admin.entity.ErpCustomer;
import com.bcsport.admin.mapper.ErpCustomerMapper;
import com.bcsport.admin.service.ErpCustomerService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.ErpCustomerVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 客户服务实现类
 */
@Service
public class ErpCustomerServiceImpl extends ServiceImpl<ErpCustomerMapper, ErpCustomer> implements ErpCustomerService {

    @Override
    public PageResult<ErpCustomerVO> pageErpCustomers(PageQuery pageQuery, ErpCustomerQueryDTO queryDTO) {
        Page<ErpCustomer> page = pageQuery.toPage();

        LambdaQueryWrapper<ErpCustomer> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getCustomerCode())) {
                queryWrapper.like(ErpCustomer::getCustomerCode, queryDTO.getCustomerCode());
            }
            if (StringUtils.hasText(queryDTO.getCustomerName())) {
                queryWrapper.like(ErpCustomer::getCustomerName, queryDTO.getCustomerName());
            }
            if (StringUtils.hasText(queryDTO.getCustomerType())) {
                queryWrapper.eq(ErpCustomer::getCustomerType, queryDTO.getCustomerType());
            }
            if (StringUtils.hasText(queryDTO.getProvince())) {
                queryWrapper.like(ErpCustomer::getProvince, queryDTO.getProvince());
            }
            if (StringUtils.hasText(queryDTO.getCity())) {
                queryWrapper.like(ErpCustomer::getCity, queryDTO.getCity());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(ErpCustomer::getStatus, queryDTO.getStatus());
            }
        }
        queryWrapper.eq(ErpCustomer::getDeleted, 0);

        // 默认排序
        if (page.orders().isEmpty()) {
            queryWrapper.orderByAsc(ErpCustomer::getSort).orderByDesc(ErpCustomer::getCreateTime);
        }

        Page<ErpCustomer> customerPage = baseMapper.selectPage(page, queryWrapper);
        List<ErpCustomerVO> voList = customerPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        PageResult<ErpCustomerVO> result = new PageResult<>();
        result.setPageNum(customerPage.getCurrent());
        result.setPageSize(customerPage.getSize());
        result.setTotal(customerPage.getTotal());
        result.setPages(customerPage.getPages());
        result.setRecords(voList);
        result.setHasPrevious(customerPage.getCurrent() > 1);
        result.setHasNext(customerPage.getCurrent() < customerPage.getPages());
        return result;
    }

    @Override
    public ErpCustomerVO getErpCustomerVOById(String id) {
        ErpCustomer customer = getById(id);
        return convertToVO(customer);
    }

    @Override
    public boolean addErpCustomer(ErpCustomerDTO dto) {
        ErpCustomer customer = BeanCopyUtils.copy(dto, ErpCustomer.class);
        return save(customer);
    }

    @Override
    public boolean updateErpCustomer(ErpCustomerDTO dto) {
        ErpCustomer customer = BeanCopyUtils.copy(dto, ErpCustomer.class);
        return updateById(customer);
    }

    @Override
    public List<ErpCustomerVO> listEnabledErpCustomers() {
        LambdaQueryWrapper<ErpCustomer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ErpCustomer::getStatus, 1);
        queryWrapper.eq(ErpCustomer::getDeleted, 0);
        queryWrapper.orderByAsc(ErpCustomer::getSort);
        List<ErpCustomer> list = list(queryWrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO 并设置显示名称
     */
    private ErpCustomerVO convertToVO(ErpCustomer customer) {
        if (customer == null) {
            return null;
        }
        ErpCustomerVO vo = BeanCopyUtils.copy(customer, ErpCustomerVO.class);
        // 设置客户类型名称
        switch (customer.getCustomerType()) {
            case "enterprise":
                vo.setCustomerTypeName("企业客户");
                break;
            case "individual":
                vo.setCustomerTypeName("个人客户");
                break;
            case "dealer":
                vo.setCustomerTypeName("经销商");
                break;
            default:
                vo.setCustomerTypeName("未知");
        }
        // 设置状态名称
        vo.setStatusName(customer.getStatus() != null && customer.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
