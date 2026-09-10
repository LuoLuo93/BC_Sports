package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bcsport.admin.common.PageQuery;
import com.bcsport.admin.common.PageResult;
import com.bcsport.admin.common.exception.BusinessException;
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
import com.bcsport.admin.vo.SportPointsBoardVO;
import com.bcsport.admin.vo.SportPointsRankVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;/**
 * BC好玩家运动积分导入实现
 * 流程骨架由 ExcelImportRunner 承担（R01），本类只声明表头别名、行映射与 MERGE 入库策略
 */
@Service
public class BcpSportPointsServiceImpl implements BcpSportPointsService {

    /** 头部统计缓存有效期兜底：数据只有导入/编辑两个写入口（写入时主动失效），TTL 只防外部直接改库的陈旧 */
    private static final long STATS_TTL_MS = 60_000L;

    /** 全表统计快照（participants / totalPoints / 缓存时刻），volatile 整体换引用保证可见性 */
    private record StatsSnapshot(long participants, long totalPoints, long cachedAt) {}

    private volatile StatsSnapshot statsCache;

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
                statsCache = null; // 导入落库后头部统计立即可见（下次请求重算）
                ctx.success(batch.size());
            }
        }).toResultMap();
    }

    @Override
    public PageResult<SysImportLog> logPage(PageQuery pageQuery) {
        return importLogService.page(ImportType.BCP_SPORT_POINTS, pageQuery);
    }

    @Override
    public SportPointsBoardVO rankBoard(String keyword, int limit) {
        SportPointsBoardVO board = new SportPointsBoardVO();
        String kw = escapeLike(keyword);
        board.setList(bcpSportPointsMapper.selectRank(limit, kw));
        // 头部指标固定为全表真实统计，不随搜索变化（搜索只影响榜单列表内容）。
        // 走缓存：十万级时 COUNT/SUM 全表扫不该每次请求都做；写入路径(导入/编辑)会主动失效
        StatsSnapshot snapshot = statsCache;
        long now = System.currentTimeMillis();
        if (snapshot == null || now - snapshot.cachedAt() > STATS_TTL_MS) {
            Map<String, Object> stats = bcpSportPointsMapper.selectRankStats();
            snapshot = new StatsSnapshot(
                    ((Number) stats.get("participants")).longValue(),
                    ((Number) stats.get("totalPoints")).longValue(), now);
            statsCache = snapshot;
        }
        board.setParticipants(snapshot.participants());
        board.setTotalPoints(snapshot.totalPoints());
        if (kw == null) {
            // 无关键字：top3 直接从榜单结果切，零额外查询
            board.setTop3(new ArrayList<>(board.getList().subList(0, Math.min(3, board.getList().size()))));
        } else {
            // 搜索时领奖台仍显示全榜前三：多一条取前3的小查询（与榜单同形状，成本可忽略）
            board.setTop3(bcpSportPointsMapper.selectRank(3, null));
        }
        return board;
    }

    /** Oracle LIKE 通配符转义（配合 XML 里的 ESCAPE '\'），用户输入的 % _ \ 按字面匹配 */
    private String escapeLike(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override
    public void updateSportPoints(Long id, String sporter, Long points) {
        if (sporter == null || sporter.trim().isEmpty()) {
            throw new BusinessException("运动员不能为空");
        }
        if (points == null) {
            throw new BusinessException("积分不能为空");
        }
        sporter = sporter.trim();
        BcpSportPoints exists = bcpSportPointsMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        // 改名时预检重名（唯一索引兜底并发窗口）
        if (!sporter.equals(exists.getSporter())) {
            Long cnt = bcpSportPointsMapper.selectCount(
                    new LambdaQueryWrapper<BcpSportPoints>().eq(BcpSportPoints::getSporter, sporter));
            if (cnt != null && cnt > 0) {
                throw new BusinessException("运动员「" + sporter + "」已存在，不能改为重名");
            }
        }
        BcpSportPoints update = new BcpSportPoints();
        update.setId(id);
        update.setSporter(sporter);
        update.setPoints(points);
        try {
            bcpSportPointsMapper.updateById(update);
            statsCache = null; // 积分/姓名变更后头部统计立即可见（下次请求重算）
        } catch (DuplicateKeyException e) {
            throw new BusinessException("运动员「" + sporter + "」已存在，不能改为重名");
        }
    }
}
