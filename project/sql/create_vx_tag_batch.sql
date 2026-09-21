-- ============================================================
-- VX_TagBatch 企微批量打标批次汇总表
-- 打标结果可见性：每次上传一行，RUNNING→DONE/FAILED，
-- 页面"打标签日志"Tab 顶部展示批次列表与未匹配统计。
-- 幂等，企微库(BC_SPORTS_QYWX @192.168.5.152)执行。
-- 注意：本文件UTF-8编码，sqlcmd执行需加 -f 65001，否则中文注释会吞行
-- ============================================================
IF OBJECT_ID('dbo.VX_TagBatch', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.VX_TagBatch (
        id BIGINT NOT NULL PRIMARY KEY,               -- IdWorker
        batchNo VARCHAR(64) NOT NULL,                 -- BATCH_yyyyMMddHHmmss
        fileName NVARCHAR(255) NULL,                  -- 上传的Excel文件名
        status VARCHAR(20) NOT NULL,                  -- RUNNING / DONE / FAILED
        totalRows INT NOT NULL DEFAULT 0,             -- 去重后有效输入行数
        successCnt INT NOT NULL DEFAULT 0,            -- 成功打标数(按 员工-客户-标签 计)
        failCnt INT NOT NULL DEFAULT 0,               -- 企微接口失败数
        unmatchedTagRows INT NOT NULL DEFAULT 0,      -- 标签库不存在该标签的行数
        unmatchedCustomerRows INT NOT NULL DEFAULT 0, -- 客户不存在或无跟进员工的行数
        errmsg NVARCHAR(500) NULL,                    -- FAILED 时原因
        startTime DATETIME NULL,
        endTime DATETIME NULL,
        createTime DATETIME NULL
    );
    CREATE NONCLUSTERED INDEX IX_VX_TagBatch_startTime
        ON dbo.VX_TagBatch (startTime DESC);
    PRINT 'VX_TagBatch created';
END
ELSE
    PRINT 'VX_TagBatch already exists, skip';
