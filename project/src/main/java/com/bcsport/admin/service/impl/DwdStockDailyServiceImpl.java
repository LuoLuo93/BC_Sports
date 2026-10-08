package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.bidwmapper.DwdStockDailyMapper;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.dto.DwdStockFlowQueryDTO;
import com.bcsport.admin.dto.DwdStockQueryDTO;
import com.bcsport.admin.entity.bi.DwStoreOption;
import com.bcsport.admin.entity.bi.DwdStockDaily;
import com.bcsport.admin.entity.bi.DwdStockFlowDetail;
import com.bcsport.admin.service.DwdStockDailyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
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

    @Override
    public DwdStockFlowDetail flowDetail(DwdStockFlowQueryDTO q) {
        // 锚点期间(最近已月结月),与正推存储过程同口径
        DwdStockFlowDetail detail = dwdStockDailyMapper.selectAnchorPeriod(q);
        if (detail == null) {
            return null;
        }
        // 月结期末数量 + 构成流水
        detail.setMonthQty(dwdStockDailyMapper.selectMonthQty(q, detail.getCPeriodId(), detail.getYearmonth()));
        List<DwdStockFlowDetail.FlowItem> flows = dwdStockDailyMapper.selectFlowItems(q, detail.getPeriodDateend());
        detail.setFlows(flows == null ? Collections.emptyList() : flows);
        BigDecimal flowSum = flows == null ? BigDecimal.ZERO
                : flows.stream().map(i -> i.getQtychange() == null ? BigDecimal.ZERO : i.getQtychange())
                       .reduce(BigDecimal.ZERO, BigDecimal::add);
        detail.setFlowSum(flowSum);
        detail.setStockQty(detail.getMonthQty().add(flowSum));
        return detail;
    }
}
