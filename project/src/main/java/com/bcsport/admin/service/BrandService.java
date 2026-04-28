package com.bcsport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.BrandDTO;
import com.bcsport.admin.dto.BrandQueryDTO;
import com.bcsport.admin.entity.Brand;
import com.bcsport.admin.vo.BrandVO;

import java.util.List;

/**
 * 品牌服务接口
 */
public interface BrandService extends IService<Brand> {
    
    /**
     * 分页查询品牌列表
     */
    PageResult<BrandVO> pageBrands(PageQuery pageQuery, BrandQueryDTO brandQueryDTO);
    
    /**
     * 根据ID获取品牌VO
     */
    BrandVO getBrandVOById(String id);

    /**
     * 新增品牌
     */
    boolean addBrand(BrandDTO brandDTO);

    /**
     * 修改品牌
     */
    boolean updateBrand(BrandDTO brandDTO);
    
    /**
     * 查询所有启用状态的品牌
     */
    List<BrandVO> listEnabledBrands();
}
