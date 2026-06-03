package com.bcsport.admin.service.sticker;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
import com.bcsport.admin.dto.sticker.StickerPrintQueryDTO;
import com.bcsport.admin.entity.sticker.StickerPrintOrder;
import com.bcsport.admin.entity.sticker.StickerPrintOrderDetail;
import com.bcsport.admin.erpmapper.BjerpProductMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderDetailMapper;
import com.bcsport.admin.mapper.sticker.StickerPrintOrderMapper;
import com.bcsport.admin.util.BeanCopyUtils;
import com.bcsport.admin.util.ShiroSecurityUtils;
import com.bcsport.admin.vo.StickerPrintOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StickerPrintService {

    @Autowired
    private StickerPrintOrderMapper orderMapper;

    @Autowired
    private StickerPrintOrderDetailMapper detailMapper;

    @Autowired
    private BarTenderPrintService barTenderPrintService;

    @Autowired
    private BjerpProductMapper bjerpProductMapper;

    public List<Map<String, Object>> searchProducts(String materialNumber, String styleNumber, String materialName, String brandId) {
        return bjerpProductMapper.searchProducts(
                escapeLike(materialNumber), escapeLike(styleNumber), escapeLike(materialName), brandId, 0, 500);
    }

    public PageResult<Map<String, Object>> searchProducts(PageQuery pageQuery, String materialNumber, String styleNumber, String materialName, String brandId) {
        int pageSize = Math.max(Math.min(pageQuery.getPageSize() != null ? pageQuery.getPageSize() : 20, 500), 1);
        int pageNum = Math.max(pageQuery.getPageNum() != null ? pageQuery.getPageNum() : 1, 1);
        int offset = (pageNum - 1) * pageSize;

        String mn = escapeLike(materialNumber), sn = escapeLike(styleNumber), mname = escapeLike(materialName);
        long total = bjerpProductMapper.countProducts(mn, sn, mname, brandId);
        List<Map<String, Object>> records = total > 0
                ? bjerpProductMapper.searchProducts(mn, sn, mname, brandId, offset, pageSize)
                : Collections.emptyList();

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setPageNum((long) pageNum);
        result.setPageSize((long) pageSize);
        result.setTotal(total);
        result.setRecords(records);
        result.setPages((long) Math.ceil((double) total / pageSize));
        result.setHasPrevious(pageNum > 1);
        result.setHasNext((long) pageNum < result.getPages());
        return result;
    }

    public List<Map<String, Object>> getBrands() {
        return bjerpProductMapper.getBrands();
    }

    public PageResult<StickerPrintOrderVO> pageOrders(PageQuery pageQuery, StickerPrintQueryDTO queryDTO) {
        LambdaQueryWrapper<StickerPrintOrder> wrapper = new LambdaQueryWrapper<StickerPrintOrder>()
            .eq(StickerPrintOrder::getDeleted, 0)
            .orderByDesc(StickerPrintOrder::getCreateTime);
        if (queryDTO != null) {
            if (queryDTO.getStatus() != null) {
                wrapper.eq(StickerPrintOrder::getStatus, queryDTO.getStatus());
            }
            if (!Boolean.TRUE.equals(queryDTO.getViewAll())) {
                String username = ShiroSecurityUtils.getCurrentUsername();
                wrapper.eq(StickerPrintOrder::getCreateBy, username);
            }
        } else {
            String username = ShiroSecurityUtils.getCurrentUsername();
            wrapper.eq(StickerPrintOrder::getCreateBy, username);
        }
        Page<StickerPrintOrder> page = orderMapper.selectPage(pageQuery.toPage(), wrapper);
        return BeanCopyUtils.copyPage(PageResult.of(page), StickerPrintOrderVO.class);
    }

    public StickerPrintOrder getOrderWithDetails(String orderId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order != null) {
            List<StickerPrintOrderDetail> details = detailMapper.selectList(
                new LambdaQueryWrapper<StickerPrintOrderDetail>()
                    .eq(StickerPrintOrderDetail::getOrderId, orderId)
                    .orderByAsc(StickerPrintOrderDetail::getSort)
            );
            order.setDetails(details);
        }
        return order;
    }

    public StickerPrintOrderVO getOrderDetailVO(String orderId) {
        StickerPrintOrder order = getOrderWithDetails(orderId);
        StickerPrintOrderVO vo = BeanCopyUtils.copy(order, StickerPrintOrderVO.class);
        if (vo != null && order.getDetails() != null) {
            vo.setDetails(BeanCopyUtils.copyList(order.getDetails(), StickerPrintOrderVO.StickerPrintOrderDetailVO.class));
        }
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String createOrder(StickerPrintOrder order) {
        String id = IdUtil.fastSimpleUUID();
        String username = ShiroSecurityUtils.getCurrentUsername();
        order.setId(id);
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setDeleted(0);
        order.setCreateBy(username);
        order.setUpdateBy(username);
        order.setApplicant(username);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);
        saveDetails(id, order.getDetails());
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(String orderId, StickerPrintOrder order) {
        StickerPrintOrder existing = orderMapper.selectById(orderId);
        if (existing == null || existing.getStatus() != 0) {
            throw new BusinessException("只有草稿状态才能编辑");
        }
        order.setId(orderId);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);

        detailMapper.delete(new LambdaQueryWrapper<StickerPrintOrderDetail>()
            .eq(StickerPrintOrderDetail::getOrderId, orderId));
        saveDetails(orderId, order.getDetails());
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitOrder(String orderId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            throw new BusinessException("只有草稿状态才能提交");
        }
        order.setStatus(1);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewOrder(String orderId, Integer status, String reviewRemark) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 1) {
            throw new BusinessException("只有待审核状态才能审核");
        }
        order.setStatus(status);
        order.setReviewer(ShiroSecurityUtils.getCurrentUsername());
        order.setReviewTime(LocalDateTime.now());
        order.setReviewRemark(reviewRemark);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(String orderId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            throw new BusinessException("只有草稿状态才能删除");
        }
        order.setDeleted(1);
        orderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean bartenderPrint(String orderId) {
        StickerPrintOrder order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 2) {
            throw new BusinessException("只有已审核的申请单才能打印");
        }
        List<StickerPrintOrderDetail> details = detailMapper.selectList(
            new LambdaQueryWrapper<StickerPrintOrderDetail>()
                .eq(StickerPrintOrderDetail::getOrderId, orderId)
                .orderByAsc(StickerPrintOrderDetail::getSort)
        );
        if (details.isEmpty()) {
            throw new BusinessException("申请单无明细");
        }
        boolean ok = barTenderPrintService.printOrder(order.getOrderNo(), details);
        if (ok) {
            order.setPrintTime(LocalDateTime.now());
            order.setPrintBy(ShiroSecurityUtils.getCurrentUsername());
            order.setUpdateTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
        return ok;
    }

    private void saveDetails(String orderId, List<StickerPrintOrderDetail> details) {
        if (details == null || details.isEmpty()) return;
        for (int i = 0; i < details.size(); i++) {
            StickerPrintOrderDetail d = details.get(i);
            d.setId(IdUtil.fastSimpleUUID());
            d.setOrderId(orderId);
            d.setSort(i);
            d.setCreateTime(LocalDateTime.now());
        }
        detailMapper.batchInsert(details);
    }

    private String generateOrderNo() {
        return "SP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%03d", (int) (Math.random() * 1000));
    }

    private String escapeLike(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
