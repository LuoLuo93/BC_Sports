-- ========================================================
-- 补建缺失的 Oracle 序列（bi/management 页面 4 个属性新增用）
-- 背景：bi/management 页面（品牌与渠道）的"新增"操作依赖序列生成主键，
--       编辑(updateById)不需要序列。因此出现"编辑OK、新增报 ORA-02289 序列不存在"。
-- 本脚本幂等：先判断序列不存在才创建，已存在则跳过，可重复执行。
-- 创建时间: 2026-07-28
-- ========================================================

-- 1. 品牌 brand（BrandMapper.selectNextId）
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_brand START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;  -- -955=已存在同名对象，忽略；其他错误抛出
END;
/

-- 2. 地区 region（RegionMapper.selectNextId）
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_region START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- 3. 渠道类型 channel-type（ChannelTypeMapper.selectNextId）
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_channel_type START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- 4. 渠道性质 channel-nature（ChannelNatureMapper.selectNextId）
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE bc_sports_seq_channel_nature START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE';
EXCEPTION WHEN OTHERS THEN
    IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

COMMIT;

-- 验证：执行后确认 4 个序列都已存在
-- SELECT sequence_name FROM user_sequences
-- WHERE sequence_name IN ('BC_SPORTS_SEQ_BRAND','BC_SPORTS_SEQ_REGION','BC_SPORTS_SEQ_CHANNEL_TYPE','BC_SPORTS_SEQ_CHANNEL_NATURE');
