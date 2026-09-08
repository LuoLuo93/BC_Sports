-- ============================================================
-- VX_CustomerTag 防重探测索引
-- VxCustomerTagMapper 的 NOT EXISTS 探测/去重按 (externalUserid, userid, tagId) 三列
-- 全表匹配，无索引时每批 MERGE 防重全表扫。企微库(BC_SPORTS_QYWX)执行，幂等。
-- ============================================================
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_VX_CustomerTag_eut' AND object_id = OBJECT_ID('dbo.VX_CustomerTag')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_VX_CustomerTag_eut
        ON dbo.VX_CustomerTag (externalUserid, userid, tagId);
    PRINT 'IX_VX_CustomerTag_eut created';
END
ELSE
    PRINT 'IX_VX_CustomerTag_eut already exists, skip';
