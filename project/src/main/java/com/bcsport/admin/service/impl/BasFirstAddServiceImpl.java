package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.qywx.BasFirstAdd;
import com.bcsport.admin.importer.BatchCtx;
import com.bcsport.admin.importer.ExcelImportRunner;
import com.bcsport.admin.importer.ExcelImportSpec;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.importer.RowCtx;
import com.bcsport.admin.qywxmapper.BasFirstAddMapper;
import com.bcsport.admin.service.BasFirstAddService;
import com.bcsport.admin.service.ImportLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户首次添加记录导入实现
 * 流程骨架由 ExcelImportRunner 承担（R01 试点），本类只声明表头别名、行映射与 MERGE 入库策略
 */
@Service
public class BasFirstAddServiceImpl implements BasFirstAddService {

    /** SQL Server 单条 INSERT 参数上限 2100，每行 5 个字段，最多 420 行，取 400 留余量 */
    private static final int BATCH_SIZE = 400;

    /** 表头别名 → 字段标识（兼容中英文表头） */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("客户id", "customerId");
        HEADER_ALIAS.put("客户ID", "customerId");
        HEADER_ALIAS.put("customer_id", "customerId");
        HEADER_ALIAS.put("customerid", "customerId");
        HEADER_ALIAS.put("首次添加时间", "firstAddTime");
        HEADER_ALIAS.put("first_add_time", "firstAddTime");
        HEADER_ALIAS.put("首次添加人部门", "firstAdderDept");
        HEADER_ALIAS.put("first_adder_dept", "firstAdderDept");
        HEADER_ALIAS.put("首次添加人店铺", "firstAdderStore");
        HEADER_ALIAS.put("first_adder_store", "firstAdderStore");
        HEADER_ALIAS.put("首次添加人", "firstAdder");
        HEADER_ALIAS.put("first_adder", "firstAdder");
    }

    @Autowired
    private BasFirstAddMapper basFirstAddMapper;

    @Autowired
    @Qualifier("qywxTransactionManager")
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExcelImportRunner importRunner;

    @Autowired
    private ImportLogService importLogService;

    @Override
    public PageResult<BasFirstAdd> page(PageQuery pageQuery, String customerId, String firstAdder) {
        Page<BasFirstAdd> page = pageQuery.toPage();
        LambdaQueryWrapper<BasFirstAdd> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(customerId)) {
            wrapper.like(BasFirstAdd::getCustomerId, customerId);
        }
        if (StringUtils.hasText(firstAdder)) {
            wrapper.like(BasFirstAdd::getFirstAdder, firstAdder);
        }
        wrapper.orderByDesc(BasFirstAdd::getFirstAddTime);
        Page<BasFirstAdd> result = basFirstAddMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        return importRunner.run(file, new ExcelImportSpec<BasFirstAdd>() {
            @Override
            public String logLabel() {
                return "Bas_FirstAdd";
            }

            @Override
            public ImportType type() {
                return ImportType.BAS_FIRST_ADD;
            }

            @Override
            public Map<String, String> headerAlias() {
                return HEADER_ALIAS;
            }

            @Override
            public int batchSize() {
                return BATCH_SIZE;
            }

            /** 按列索引映射到实体（表头未识别时按固定顺序兜底） */
            @Override
            public BasFirstAdd mapRow(RowCtx ctx) {
                BasFirstAdd e = new BasFirstAdd();
                e.setCustomerId(ctx.str("customerId", 0));
                e.setFirstAddTime(ctx.str("firstAddTime", 1));
                e.setFirstAdderDept(ctx.str("firstAdderDept", 2));
                e.setFirstAdderStore(ctx.str("firstAdderStore", 3));
                e.setFirstAdder(ctx.str("firstAdder", 4));
                if (e.getCustomerId() == null) {
                    throw new IllegalArgumentException("客户id不能为空");
                }
                return e;
            }

            /** 文件内去重：同一缓冲内相同 customerId 只保留最后一条 */
            @Override
            public String dedupKey(BasFirstAdd entity) {
                return entity.getCustomerId();
            }

            /** MERGE upsert 入库（独立短事务），一条 SQL 同时处理插入和更新，无需预加载全表 ID */
            @Override
            public void onBatch(List<BasFirstAdd> batch, BatchCtx ctx) {
                txTemplate.execute(status -> {
                    basFirstAddMapper.mergeBatch(batch);
                    return null;
                });
                ctx.success(batch.size());
            }
        }).toResultMap();
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.BAS_FIRST_ADD, pageQuery);
    }
}
