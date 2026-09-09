package com.bcsport.admin.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导入结果（替代各服务的 buildResult）。
 * 状态语义：total==0 → FAILED；fail==0 → SUCCESS；否则 PARTIAL。
 */
public final class ImportOutcome {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";

    private final int total;
    private final int success;
    private final int fail;
    private final List<String> errors;
    private final String status;

    public ImportOutcome(int total, int success, int fail, List<String> errors) {
        this.total = total;
        this.success = success;
        this.fail = fail;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.status = total == 0 ? STATUS_FAILED : (fail == 0 ? STATUS_SUCCESS : STATUS_PARTIAL);
    }

    /** 文件在读取前即被拒绝（如非 Excel 格式） */
    public static ImportOutcome rejected(String reason) {
        return new ImportOutcome(0, 0, 0, Collections.singletonList(reason));
    }

    /** 标准状态推导（total==0→FAILED、fail==0→SUCCESS、否则 PARTIAL），供保留自有流程的模块复用 */
    public static ImportOutcome of(int total, int success, int fail, List<String> errors) {
        return new ImportOutcome(total, success, fail, errors);
    }

    /**
     * 自定义状态（不走计数推导）——供保留自有流程的特殊模块使用，
     * 如数仓销售的"解析中断已读部分已入库"需记 PARTIAL 而计数推导不出。
     */
    public static ImportOutcome withStatus(int total, int success, int fail, List<String> errors, String status) {
        return new ImportOutcome(total, success, fail, errors, status);
    }

    private ImportOutcome(int total, int success, int fail, List<String> errors, String status) {
        this.total = total;
        this.success = success;
        this.fail = fail;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.status = status;
    }

    public int getTotal() {
        return total;
    }

    public int getSuccess() {
        return success;
    }

    public int getFail() {
        return fail;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getStatus() {
        return status;
    }

    /** 接口响应报文，键顺序 total/success/fail/errors 与历史一致 */
    public Map<String, Object> toResultMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }
}
