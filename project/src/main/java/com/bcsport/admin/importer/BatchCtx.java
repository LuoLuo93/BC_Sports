package com.bcsport.admin.importer;

/**
 * 批入库回调可用的计数通道。
 * Spec 在写库成功后调用 success(n)；批内业务错误调用 error(msg)（引擎按上限截断）。
 * onBatch 抛出的异常由引擎统一捕获并记为"批量入库失败"，Spec 无需自己 try/catch。
 */
public interface BatchCtx {

    void success(int n);

    void error(String msg);
}
