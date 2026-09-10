package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.entity.SysImportLog;
import com.bcsport.admin.entity.bcp.BcpSportPoints;
import com.bcsport.admin.importer.BatchCtx;
import com.bcsport.admin.importer.ExcelImportRunner;
import com.bcsport.admin.importer.ExcelImportSpec;
import com.bcsport.admin.importer.ImportType;
import com.bcsport.admin.importer.RowCtx;
import com.bcsport.admin.mapper.BcpSportPointsMapper;
import com.bcsport.admin.service.BcpSportPointsService;
import com.bcsport.admin.service.ImportLogService;
import com.bcsport.admin.util.ShiroSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BC好玩家运动积分导入实现
 * 流程骨架由 ExcelImportRunner 承担（R01），本类只声明表头别名、行映射与 MERGE 入库策略
 */
@Service
public class BcpSportPointsServiceImpl implements BcpSportPointsService {

    /** 表头别名 → 字段标识 */
    private static final Map<String, String> HEADER_ALIAS = new HashMap<>();
    static {
        HEADER_ALIAS.put("运动员", "sporter");
        HEADER_ALIAS.put("玩家", "sporter");
        HEADER_ALIAS.put("好玩家", "sporter");
        HEADER_ALIAS.put("姓名", "sporter");
        HEADER_ALIAS.put("sporter", "sporter");
        HEADER_ALIAS.put("Sporter", "sporter");
        HEADER_ALIAS.put("SPORTER", "sporter");
        HEADER_ALIAS.put("积分", "points");
        HEADER_ALIAS.put("分数", "points");
        HEADER_ALIAS.put("points", "points");
        HEADER_ALIAS.put("Points", "points");
        HEADER_ALIAS.put("POINTS", "points");
    }

    @Autowired
    private BcpSportPointsMapper bcpSportPointsMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExcelImportRunner importRunner;

    @Autowired
    private ImportLogService importLogService;

    @Override
    public PageResult<BcpSportPoints> page(PageQuery pageQuery, String sporter) {
        Page<BcpSportPoints> page = pageQuery.toPage();
        LambdaQueryWrapper<BcpSportPoints> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(sporter)) {
            wrapper.like(BcpSportPoints::getSporter, sporter);
        }
        wrapper.orderByDesc(BcpSportPoints::getId);
        Page<BcpSportPoints> result = bcpSportPointsMapper.selectPage(page, wrapper);
        return PageResult.of(result);
    }

    @Override
    public Map<String, Object> importFromExcel(MultipartFile file) throws Exception {
        return importRunner.run(file, new ExcelImportSpec<BcpSportPoints>() {
            @Override
            public String logLabel() {
                return "BcpSportPoints";
            }

            @Override
            public ImportType type() {
                return ImportType.BCP_SPORT_POINTS;
            }

            @Override
            public Map<String, String> headerAlias() {
                return HEADER_ALIAS;
            }

            @Override
            public void validateHeaders(Map<String, Integer> columnIndex) {
                List<String> missing = new ArrayList<>();
                if (!columnIndex.containsKey("sporter")) missing.add("运动员(sporter)");
                if (!columnIndex.containsKey("points")) missing.add("积分(points)");
                if (!missing.isEmpty()) {
                    throw new IllegalArgumentException("Excel缺少必需列：" + String.join("、", missing) + "，请检查表头");
                }
            }

            @Override
            public BcpSportPoints mapRow(RowCtx ctx) {
                String sporter = ctx.str("sporter", 0);
                if (!StringUtils.hasText(sporter)) throw new IllegalArgumentException("运动员不能为空");
                // 不用 longOrNull：BigDecimal.longValue() 会把 12.7 静默截成 12，这里按"值必须是整数"严格校验
                String rawPoints = ctx.str("points", 1);
                if (rawPoints == null) throw new IllegalArgumentException("积分不能为空");
                BigDecimal parsed;
                try {
                    parsed = new BigDecimal(rawPoints).stripTrailingZeros();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("积分必须是数字（当前值: " + rawPoints + "）");
                }
                if (parsed.scale() > 0) {
                    throw new IllegalArgumentException("积分必须是整数（当前值: " + rawPoints + "）");
                }
                Long points;
                try {
                    points = parsed.longValueExact();
                } catch (ArithmeticException e) {
                    throw new IllegalArgumentException("积分数值超出可导入范围（当前值: " + rawPoints + "）");
                }
                BcpSportPoints e = new BcpSportPoints();
                e.setSporter(sporter);
                e.setPoints(points);
                return e;
            }

            /** 文件内去重：同一运动员后行覆盖前行 */
            @Override
            public String dedupKey(BcpSportPoints entity) {
                return entity.getSporter();
            }

            @Override
            public void onBatch(List<BcpSportPoints> batch, BatchCtx ctx) {
                String currentUser = ShiroSecurityUtils.getCurrentUsername();
                batch.forEach(e -> {
                    e.setCreateBy(currentUser);
                    e.setUpdateBy(currentUser);
                });
                new TransactionTemplate(transactionManager).execute(status -> {
                    bcpSportPointsMapper.mergeBatch(batch);
                    return null;
                });
                ctx.success(batch.size());
            }
        }).toResultMap();
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.BCP_SPORT_POINTS, pageQuery);
    }
}
