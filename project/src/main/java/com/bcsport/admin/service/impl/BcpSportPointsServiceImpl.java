package com.bcsport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;/**
 * BC好玩家运动积分导入实现
 * 流程骨架由 ExcelImportRunner 承担（R01），本类只声明表头别名、行映射与 MERGE 入库策略
 */
@Slf4j
@Service
public class BcpSportPointsServiceImpl implements BcpSportPointsService {

    /** 头部统计缓存有效期兜底：数据只有导入/编辑两个写入口（写入时主动失效），TTL 只防外部直接改库的陈旧 */
    private static final long STATS_TTL_MS = 60_000L;

    /** 全表统计快照（participants / totalPoints / 缓存时刻），volatile 整体换引用保证可见性 */
    private record StatsSnapshot(long participants, BigDecimal totalPoints, long cachedAt) {}

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
                // 积分只保留整数：Excel 可带小数，入库前四舍五入取整
                String rawPoints = ctx.str("points", 1);
                if (rawPoints == null) throw new IllegalArgumentException("积分不能为空");
                BigDecimal parsed;
                try {
                    parsed = new BigDecimal(rawPoints);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("积分必须是数字（当前值: " + rawPoints + "）");
                }
                parsed = parsed.setScale(0, RoundingMode.HALF_UP);
                // Oracle NUMBER 最多 38 位有效数字，超了入库必报错，提前拦成行级错误
                if (parsed.precision() > 38) {
                    throw new IllegalArgumentException("积分数值超出可导入范围（当前值: " + rawPoints + "）");
                }
                BcpSportPoints e = new BcpSportPoints();
                e.setSporter(sporter);
                e.setPoints(parsed);
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
            Object totalVal = stats.get("totalPoints");
            BigDecimal totalPoints = totalVal instanceof BigDecimal bd
                    ? bd : new BigDecimal(String.valueOf(totalVal));
            snapshot = new StatsSnapshot(
                    ((Number) stats.get("participants")).longValue(),
                    totalPoints, now);
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
    public void updateSportPoints(Long id, String sporter, BigDecimal points) {
        if (sporter == null || sporter.trim().isEmpty()) {
            throw new BusinessException("运动员不能为空");
        }
        if (points == null) {
            throw new BusinessException("积分不能为空");
        }
        // 与导入同口径：只保留整数，四舍五入取整
        points = points.setScale(0, RoundingMode.HALF_UP);
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

    // ===== 自定义头像（管理员代传） =====
    // 存 uploads/avatar 子目录（/images/avatar/** 已在 Shiro 单独放行 anon，其余 /images/** 业务文件仍需登录）
    // 换头像/清除时旧文件删除，文件名带时间戳天然破缓存；Excel 重导入走 MERGE 只更新积分，不碰头像

    private static final String AVATAR_DIR = "avatar";
    private static final String AVATAR_URL_PREFIX = "/images/avatar/";
    private static final long AVATAR_MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> AVATAR_EXTS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${bc.upload.path:E:/work/BC_Sport/uploads}")
    private String uploadBasePath;

    @Override
    public String saveAvatar(Long id, MultipartFile file) {
        BcpSportPoints row = bcpSportPointsMapper.selectById(id);
        if (row == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        if (file.getSize() > AVATAR_MAX_BYTES) {
            throw new BusinessException("图片不能超过 5MB");
        }
        String ext = extOf(file.getOriginalFilename());
        if (!AVATAR_EXTS.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/webp 格式图片");
        }
        // 内容校验：必须能解码成真图片（webp Java 标准库不解码，只做扩展名/大小限制）
        if (!"webp".equals(ext)) {
            try (InputStream in = file.getInputStream()) {
                if (ImageIO.read(in) == null) {
                    throw new BusinessException("图片内容无法解析，请换一张");
                }
            } catch (IOException e) {
                throw new BusinessException("读取图片失败，请重试");
            }
        }
        String filename = "avatar_" + id + "_" + System.currentTimeMillis() + "." + ext;
        Path dir = Paths.get(uploadBasePath, AVATAR_DIR);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toAbsolutePath().toFile());
        } catch (IOException e) {
            log.error("[SportPoints] 头像文件写入失败: id={}", id, e);
            throw new BusinessException("头像保存失败，请重试");
        }
        String url = AVATAR_URL_PREFIX + filename;
        BcpSportPoints update = new BcpSportPoints();
        update.setId(id);
        update.setAvatarUrl(url);
        bcpSportPointsMapper.updateById(update); // updateFill 自动带 update_time/update_by
        deleteAvatarFileQuietly(row.getAvatarUrl());
        return url;
    }

    @Override
    public void clearAvatar(Long id) {
        BcpSportPoints row = bcpSportPointsMapper.selectById(id);
        if (row == null) {
            throw new BusinessException("记录不存在或已删除，请刷新列表");
        }
        if (!StringUtils.hasText(row.getAvatarUrl())) {
            return; // 本来就没有头像，幂等成功
        }
        // 置 NULL 需走 UpdateWrapper 显式 set（updateById 的 null 字段默认不参与更新）
        bcpSportPointsMapper.update(null, new LambdaUpdateWrapper<BcpSportPoints>()
                .eq(BcpSportPoints::getId, id)
                .set(BcpSportPoints::getAvatarUrl, null)
                .set(BcpSportPoints::getUpdateTime, LocalDateTime.now())
                .set(BcpSportPoints::getUpdateBy,
                        ShiroSecurityUtils.getCurrentUsername() != null ? ShiroSecurityUtils.getCurrentUsername() : "unknown"));
        deleteAvatarFileQuietly(row.getAvatarUrl());
    }

    /** 删除磁盘上的旧头像文件（尽力而为，失败只记日志不影响主流程） */
    private void deleteAvatarFileQuietly(String url) {
        if (!StringUtils.hasText(url)) return;
        // 只认本功能自己的固定前缀，且拒绝路径穿越，避免任意文件删除
        if (!url.startsWith(AVATAR_URL_PREFIX) || url.contains("..")) return;
        try {
            Files.deleteIfExists(Paths.get(uploadBasePath, AVATAR_DIR, url.substring(AVATAR_URL_PREFIX.length())));
        } catch (IOException e) {
            log.warn("[SportPoints] 旧头像文件删除失败: {}", url);
        }
    }

    private static String extOf(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }
}
