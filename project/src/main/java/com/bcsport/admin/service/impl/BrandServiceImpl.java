package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.BrandDTO;
import com.bcsport.admin.dto.BrandQueryDTO;
import com.bcsport.admin.entity.Brand;
import com.bcsport.admin.mapper.BrandMapper;
import com.bcsport.admin.service.BrandService;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.vo.BrandVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 品牌服务实现类 */
@Service
public class BrandServiceImpl extends ServiceImpl<BrandMapper, Brand> implements BrandService {

    @Override
    public PageResult<BrandVO> pageBrands(PageQuery pageQuery, BrandQueryDTO brandQueryDTO) {
        Page<Brand> page = pageQuery.toPage();
        
        LambdaQueryWrapper<Brand> queryWrapper = new LambdaQueryWrapper<>();
        if (brandQueryDTO != null) {
            if (StringUtils.hasText(brandQueryDTO.getBrandName())) {
                queryWrapper.like(Brand::getBrandName, brandQueryDTO.getBrandName());
            }
            if (brandQueryDTO.getStatus() != null) {
                queryWrapper.eq(Brand::getStatus, brandQueryDTO.getStatus());
            }
        }
        queryWrapper.eq(Brand::getDeleted, 0);
        
        // If PageQuery didn't specify order, use default
        if (page.orders().isEmpty()) {
            queryWrapper.orderByAsc(Brand::getSort).orderByDesc(Brand::getCreateTime);
        }
        
        Page<Brand> brandPage = baseMapper.selectPage(page, queryWrapper);
        return BeanCopyUtils.copyPage(PageResult.of(brandPage), BrandVO.class);
    }

    @Override
    public BrandVO getBrandVOById(String id) {
        Brand brand = getById(id);
        return BeanCopyUtils.copy(brand, BrandVO.class);
    }

    @Override
    public boolean addBrand(BrandDTO brandDTO) {
        Brand brand = BeanCopyUtils.copy(brandDTO, Brand.class);
        return save(brand);
    }

    @Override
    public boolean updateBrand(BrandDTO brandDTO) {
        Brand brand = BeanCopyUtils.copy(brandDTO, Brand.class);
        return updateById(brand);
    }

    @Override
    public List<BrandVO> listEnabledBrands() {
        LambdaQueryWrapper<Brand> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Brand::getStatus, 1);
        queryWrapper.eq(Brand::getDeleted, 0);
        queryWrapper.orderByAsc(Brand::getSort);
        List<Brand> list = list(queryWrapper);
        return BeanCopyUtils.copyList(list, BrandVO.class);
    }
}
