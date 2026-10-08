package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.DwdStockDailyMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import com.bcsport.admin.service.DwdStockDailyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数仓库存查询实现（查询走 bidw 数据源）
 */
@Service
public class DwdStockDailyServiceImpl implements DwdStockDailyService {

    @Autowired
    private DwdStockDailyMapper dwdStockDailyMapper;

    @Override
    public PageResult<DwdStockDaily> page(PageQuery pageQuery, DwdStockQueryDTO queryDTO) {
        Page<DwdStockDaily> result = dwdStockDailyMapper.selectPage(pageQuery.toPage(), queryDTO);
        return PageResult.of(result);
    }

    @Override
    public List<DwStoreOption> stores() {
        return dwdStockDailyMapper.selectStores();
    }
}
