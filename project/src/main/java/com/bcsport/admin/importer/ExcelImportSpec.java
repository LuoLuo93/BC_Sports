package com.bcsport.admin.importer;

import java.util.List;
import java.util.Map;

/**
 * 一个导入模块的全部差异点。新增导入只需实现本接口并交给 {@link ExcelImportRunner#run}，
 * 流程骨架（格式探测/表头识别/逐行解析/熔断/分批/计错/收尾/写统一日志）全部由引擎承担。
 *
 * <p>行级错误文案约定：{@link #mapRow} 抛 {@link IllegalArgumentException} 视为业务校验失败，
 * 引擎记为 "第N行：&lt;message&gt;"；其他异常（含 {@link NumberFormatException}，虽是 IAE 子类但属解析失败）
 * 记为 "第N行：解析异常 - &lt;message&gt;"。返回 null 表示静默跳过该行（不计入 total）。
 *
 * @param <T> 行实体
 */
public interface ExcelImportSpec<T> {

    /** 日志里的模块名（如 "Bas_FirstAdd"），仅用于日志区分 */
    String logLabel();

    /** 统一导入日志的模块标识 */
    ImportType type();

    /** 表头别名 → 字段名（兼容中英文/新旧模板表头） */
    Map<String, String> headerAlias();

    /** 单行映射为实体 */
    T mapRow(RowCtx ctx) throws Exception;

    /**
     * 表头首次识别成功后立即校验（在读任何数据行之前）。缺少必需列时抛
     * {@link IllegalArgumentException}("Excel缺少必需列：...")，引擎中止读取并按拒绝处理——
     * 避免按固定列序兜底把垃圾数据写入库之后才报缺列。
     */
    default void validateHeaders(Map<String, Integer> columnIndex) {
    }

    /**
     * 一批实体入库。各模块自行决定策略（MERGE 短事务 / 纯插入 / 先攒清单读完再两段式写）。
     * 写库成功后调用 ctx.success(batch.size())；抛出异常由引擎记为"批量入库失败"并丢弃本批。
     */
    void onBatch(List<T> batch, BatchCtx ctx);

    /** 非 null 则在同一未 flush 缓冲内按此 key 去重（保留最后一条）；每次 flush 后去重集清空 */
    default String dedupKey(T entity) {
        return null;
    }

    /** 缓冲满此数即触发 onBatch；SQL Server 单条 INSERT 2100 参数上限按列数自定 */
    default int batchSize() {
        return 500;
    }

    /** 行数安全上限，超出的行计入 total 并记错，不再解析 */
    default int maxRows() {
        return 2_000_000;
    }

    /** 读取与入库全部结束后、生成最终结果前回调（追加模块特有的收尾提示用） */
    default void onFinish(int total, int success, int fail, List<String> errors) {
    }
}
