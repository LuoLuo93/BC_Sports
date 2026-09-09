package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bi.GoodsOldNew;
import com.bcsport.admin.importer.BatchCtx;
import com.bcsport.admin.importer.ExcelImportRunner;
import com.bcsport.admin.importer.ExcelImportSpec;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.importer.RowCtx;
import com.bcsport.admin.mapper.GoodsOldNewMapper;
import com.bcsport.admin.service.GoodsOldNewService;
import com.bcsport.admin.service.ImportLogService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 货品资料导入实现
 * 流程骨架由 ExcelImportRunner 承担（R01），本类只声明表头别名、行映射与 MERGE 入库策略
 */
@Service
public class GoodsOldNewServiceImpl implements GoodsOldNewService {

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("品牌", "brand");
        HEADER_ALIAS.put("brand", "brand");
        HEADER_ALIAS.put("Brand", "brand");
        HEADER_ALIAS.put("货号", "articleNo");
        HEADER_ALIAS.put("article_no", "articleNo");
        HEADER_ALIAS.put("articleno", "articleNo");
        HEADER_ALIAS.put("产品季", "season");
        HEADER_ALIAS.put("season", "season");
        HEADER_ALIAS.put("Season", "season");
        HEADER_ALIAS.put("货品分类", "category");
        HEADER_ALIAS.put("新旧货", "category");
        HEADER_ALIAS.put("category", "category");
        HEADER_ALIAS.put("Category", "category");
    }

    @Autowired
    private GoodsOldNewMapper goodsOldNewMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExcelImportRunner importRunner;

    @Autowired
    private ImportLogService importLogService;

    @Override
    public PageResult<GoodsOldNew> page(PageQuery pageQuery, String brand, String articleNo) {
        Page<GoodsOldNew> page = pageQuery.toPage();
        LambdaQueryWrapper<GoodsOldNew> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(brand)) {
            wrapper.like(GoodsOldNew::getBrand, brand);
        }
        if (StringUtils.hasText(articleNo)) {
            wrapper.like(GoodsOldNew::getArticleNo, articleNo);
        }
        wrapper.orderByDesc(GoodsOldNew::getId);
        Page<GoodsOldNew> result = goodsOldNewMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        return importRunner.run(file, new ExcelImportSpec<GoodsOldNew>() {
            @Override
            public String logLabel() {
                return "GoodsOldNew";
            }

            @Override
            public ImportType type() {
                return ImportType.GOODS_OLD_NEW;
            }

            @Override
            public Map<String, String> headerAlias() {
                return HEADER_ALIAS;
            }

            @Override
            public void validateHeaders(Map<String, Integer> columnIndex) {
                List<String> missing = new ArrayList<>();
                if (!columnIndex.containsKey("brand")) missing.add("品牌");
                if (!columnIndex.containsKey("articleNo")) missing.add("货号");
                if (!columnIndex.containsKey("season")) missing.add("产品季");
                if (!columnIndex.containsKey("category")) missing.add("新旧货");
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Excel缺少必需列：" + String.join("、", missing) + "，请检查表头");
                }
            }

            @Override
            public GoodsOldNew mapRow(RowCtx ctx) {
                GoodsOldNew e = new GoodsOldNew();
                e.setBrand(ctx.str("brand", 0));
                e.setArticleNo(ctx.str("articleNo", 1));
                e.setSeason(ctx.str("season", 2));
                e.setCategory(ctx.str("category", 3));
                if (!StringUtils.hasText(e.getBrand())) throw new IllegalArgumentException("品牌不能为空");
                if (!StringUtils.hasText(e.getArticleNo())) throw new IllegalArgumentException("货号不能为空");
                if (!StringUtils.hasText(e.getSeason())) throw new IllegalArgumentException("产品季不能为空");
                return e;
            }

            /** 文件内去重：articleNo+season 组合键(一个货号一个产品季只一行, 后行覆盖前行) */
            @Override
            public String dedupKey(GoodsOldNew entity) {
                return entity.getArticleNo() + "|" + entity.getSeason();
            }

            @Override
            public void onBatch(List<GoodsOldNew> batch, BatchCtx ctx) {
                String currentUser = ShiroSecurityUtils.getCurrentUsername();
                batch.forEach(e -> {
                    e.setCreateBy(currentUser);
                    e.setUpdateBy(currentUser);
                });
                new TransactionTemplate(transactionManager).execute(status -> {
                    goodsOldNewMapper.mergeBatch(batch);
                    return null;
                });
                ctx.success(batch.size());
            }
        }).toResultMap();
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.GOODS_OLD_NEW, pageQuery);
    }
}
