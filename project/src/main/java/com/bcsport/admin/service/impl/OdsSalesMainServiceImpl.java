package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.OdsSalesMainMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.OdsSalesMainQueryDTO;
import com.bcsport.admin.dto.OdsSalesMainUpdateDTO;
import com.bcsport.admin.entity.bi.OdsSalesMain;
import com.bcsport.admin.service.OdsSalesMainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数仓销售查看实现（纯查询，走 bidw 数据源）
 */
@Slf4j
@Service
public class OdsSalesMainServiceImpl implements OdsSalesMainService {

    @Autowired
    private OdsSalesMainMapper odsSalesMainMapper;

    @Override
    public PageResult<OdsSalesMain> page(PageQuery pageQuery, OdsSalesMainQueryDTO queryDTO) {
        Page<OdsSalesMain> result = odsSalesMainMapper.selectPage(pageQuery.toPage(), queryDTO);
        return PageResult.of(result);
    }

    @Override
    public boolean update(OdsSalesMainUpdateDTO dto) {
        return odsSalesMainMapper.updateRow(dto) > 0;
    }
}
