-- ============================================================
-- VX_CustomerTag 增加打标结果状态 / 失败原因 / 打标动作
-- 之前只落成功记录，接口失败与未匹配(标签不存在/客户无跟进人)只存在于服务端日志；
-- 加列后失败与未匹配也记流水，日志页可按状态筛选定位问题行。
-- status:   1=成功(默认,旧行兼容) 0=企微接口失败 2=标签未匹配 3=客户未匹配
-- tagAction: ADD=打标(默认) REMOVE=移除标签
-- 幂等，企微库(BC_SPORTS_QYWX @192.168.5.152)执行。
-- 注意：本文件UTF-8编码，sqlcmd执行需加 -f 65001，否则中文注释可能吞行
-- ============================================================
IF COL_LENGTH('dbo.VX_CustomerTag', 'status') IS NULL
BEGIN
    ALTER TABLE dbo.VX_CustomerTag ADD status INT NOT NULL DEFAULT 1;
    PRINT 'VX_CustomerTag.status added';
END
ELSE
    PRINT 'VX_CustomerTag.status already exists, skip';

IF COL_LENGTH('dbo.VX_CustomerTag', 'tagAction') IS NULL
BEGIN
    ALTER TABLE dbo.VX_CustomerTag ADD tagAction VARCHAR(10) NOT NULL DEFAULT 'ADD';
    PRINT 'VX_CustomerTag.tagAction added';
END
ELSE
    PRINT 'VX_CustomerTag.tagAction already exists, skip';

IF COL_LENGTH('dbo.VX_CustomerTag', 'errmsg') IS NULL
BEGIN
    ALTER TABLE dbo.VX_CustomerTag ADD errmsg NVARCHAR(500) NULL;
    PRINT 'VX_CustomerTag.errmsg added';
END
ELSE
    PRINT 'VX_CustomerTag.errmsg already exists, skip';

-- 未匹配记录没有跟进人/标签ID，userid/tagId 需允许 NULL
-- tagId 上有索引 idx_ct_tag，需先删后改再重建
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.VX_CustomerTag') AND name='userid' AND is_nullable=0)
    ALTER TABLE dbo.VX_CustomerTag ALTER COLUMN userid NVARCHAR(128) NULL;
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID('dbo.VX_CustomerTag') AND name='tagId' AND is_nullable=0)
BEGIN
    IF EXISTS (SELECT 1 FROM sys.indexes WHERE name='idx_ct_tag' AND object_id=OBJECT_ID('dbo.VX_CustomerTag'))
        DROP INDEX idx_ct_tag ON dbo.VX_CustomerTag;
    ALTER TABLE dbo.VX_CustomerTag ALTER COLUMN tagId NVARCHAR(200) NULL;
    CREATE INDEX idx_ct_tag ON dbo.VX_CustomerTag (tagId);
END
PRINT 'userid/tagId nullable done';
