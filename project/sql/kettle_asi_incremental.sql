--===========================================================================
-- Kettle 增量抽数 —— 属性实例维表(颜色/尺码) 按主键 id 水位增量
-- 表: ERP(BOSNDS3.M_ATTRIBUTESETINSTANCE) → 数仓(BI_DW.ODS_M_ATTRIBUTESETINSTANCE)
-- 挂点: 加进现有库存 ETL 作业(与 kettle_extract_5tables / kettle_fin_param_3sql 同链路)
--
-- ★ 为什么按 id 增量安全:
--   实例表 append-only(新颜色×尺码组合=新 id,历史行不改不删),
--   id 为序列基本单调,按 id 水位不会漏;无 modifieddate 依赖。
--
-- 变量: ASI_LOWERLIMIT / ASI_UPPERLIMIT(Kettle 变量,%%VAR%% 语法)
-- 三段式(对齐 kettle_fin_param_3sql.sql 的幂等模式):
--   ① 数仓侧取水位 → 变量(表输入 + 设置变量)
--   ② 数仓侧删该 id 段旧数据(幂等,通常 0 行)
--   ③ 源库 SELECT → 数仓表输出
--===========================================================================

------------------------------------------------------------------------------
-- SQL1(数仓 BI_DW 执行): 取增量下界(已同步最大 id + 1)
--   Kettle 配置: 表输入取一行两列 → 设置变量组件
--   手动全量重灌时跳过此步,直接把下界设为 0
------------------------------------------------------------------------------
SELECT NVL(MAX(id), 0) + 1 AS lower_limit,
       999999999         AS upper_limit
FROM ods_m_attributesetinstance;


------------------------------------------------------------------------------
-- SQL2(数仓 BI_DW 执行): 删除该 id 段旧数据(幂等保障,重跑不撞主键)
------------------------------------------------------------------------------
DELETE FROM ods_m_attributesetinstance
WHERE id BETWEEN %%ASI_LOWERLIMIT%% AND %%ASI_UPPERLIMIT%%;


------------------------------------------------------------------------------
-- SQL3(源库 BOSNDS3 执行 SELECT,结果写入数仓 ODS_M_ATTRIBUTESETINSTANCE):
--   只抽 id 段内的 4 个业务列(etl_load_time 走目标表默认值 SYSDATE)
------------------------------------------------------------------------------
SELECT
    id,
    m_attributeset_id,
    value1_code,
    value2_code
FROM m_attributesetinstance
WHERE id BETWEEN %%ASI_LOWERLIMIT%% AND %%ASI_UPPERLIMIT%%;
