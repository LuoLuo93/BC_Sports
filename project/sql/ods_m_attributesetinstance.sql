--===========================================================================
-- 表名: ODS_M_ATTRIBUTESETINSTANCE
-- 来源: 伯俊ERP(BOSNDS3.M_ATTRIBUTESETINSTANCE)  /  目标: 数仓 BI_DW
-- 用途: 库存查询页的颜色/尺码解析维表
--   dwd_stock_daily 的 m_attributesetinstance_id 只存属性实例ID;
--   ERP 在实例表上冗余了两列:
--     VALUE1_CODE = 颜色名(如 甘草色/炭黑色)
--     VALUE2_CODE = 尺码(如 170/110/M)
--   (实测 541 万实例 100% 有值;关联键 ID 即主键)
--   ★ 只建这张独立维表,不改 dwd_stock_daily 及库存链路任何表结构
--
-- 消费方: DwdStockDailyMapper.xml 批次明细分支
--   LEFT JOIN ODS_M_ATTRIBUTESETINSTANCE asi ON asi.ID = d.M_ATTRIBUTESETINSTANCE_ID
--   (LEFT: 维表未同步到的新实例颜色/尺码显示为空,不丢库存行)
--
-- 同步口径: 全量重灌(TRUNCATE + 全量 INSERT,幂等)
--   实例表 append-only 随新品增长,无 UPDATE/DELETE;
--   新品上架后新实例在维表缺颜色/尺码时,重跑一次灌数即可补齐。
--   灌数工具: tmp/EtlAsi.java(ERP→数仓双连接流式批量搬运,541万行约3~6分钟)
--===========================================================================
CREATE TABLE ods_m_attributesetinstance
(
    id                  NUMBER(10)      NOT NULL,   -- 属性实例ID(=dwd_stock_daily.m_attributesetinstance_id)
    m_attributeset_id   NUMBER(10),                  -- 属性集ID(19种矩阵集,排查用)
    value1_code         VARCHAR2(60),                -- ★颜色名
    value2_code         VARCHAR2(60),                -- ★尺码
    etl_load_time       DATE DEFAULT SYSDATE,        -- ETL入仓时间
    CONSTRAINT pk_ods_m_asi PRIMARY KEY (id)
);

COMMENT ON TABLE  ods_m_attributesetinstance              IS '属性实例维表(颜色/尺码解析,源: BOSNDS3.M_ATTRIBUTESETINSTANCE 冗余列)';
COMMENT ON COLUMN ods_m_attributesetinstance.id           IS '属性实例ID(批次/色码,join dwd_stock_daily)';
COMMENT ON COLUMN ods_m_attributesetinstance.m_attributeset_id IS '属性集ID(19种矩阵集)';
COMMENT ON COLUMN ods_m_attributesetinstance.value1_code  IS '★颜色名(如 甘草色/炭黑色)';
COMMENT ON COLUMN ods_m_attributesetinstance.value2_code  IS '★尺码(如 170/110/M)';
COMMENT ON COLUMN ods_m_attributesetinstance.etl_load_time IS 'ETL入仓时间';

-- 本表已在 BI_DW 建好并全量灌入(2026-10-08,541万行 119 秒),此处为存档。
--
-- 重灌工具源码(附录,tmp/ 不入库;任何机器可按此重建,JDK8+ + ojdbc8):
-- ============================================================================
-- import java.sql.*;
-- public class EtlAsi {
--     public static void main(String[] args) throws Exception {
--         String erpUrl = "jdbc:oracle:thin:@10.0.1.22:1521:orcl";   // ERP正式库 BOSNDS3/abc123
--         String dwUrl  = "jdbc:oracle:thin:@192.168.5.177:1521:orcl"; // 数仓 BI_DW/123456
--         Class.forName("oracle.jdbc.OracleDriver");
--         try (Connection erp = DriverManager.getConnection(erpUrl, "bosnds3", "abc123");
--              Connection dw  = DriverManager.getConnection(dwUrl, "bi_dw", "123456")) {
--             dw.setAutoCommit(false);
--             try (Statement st = dw.createStatement()) { st.execute("TRUNCATE TABLE ods_m_attributesetinstance"); }
--             dw.commit();
--             String ins = "INSERT INTO ods_m_attributesetinstance (id, m_attributeset_id, value1_code, value2_code) VALUES (?,?,?,?)";
--             String sel = "SELECT id, m_attributeset_id, value1_code, value2_code FROM m_attributesetinstance";
--             try (Statement selSt = erp.createStatement();
--                  PreparedStatement ps = dw.prepareStatement(ins)) {
--                 selSt.setFetchSize(5000);
--                 ResultSet rs = selSt.executeQuery(sel);
--                 int batch = 0; long n = 0; long t0 = System.currentTimeMillis();
--                 while (rs.next()) {
--                     ps.setLong(1, rs.getLong(1)); ps.setLong(2, rs.getLong(2));
--                     ps.setString(3, rs.getString(3)); ps.setString(4, rs.getString(4));
--                     ps.addBatch();
--                     if (++batch >= 5000) { ps.executeBatch(); dw.commit(); batch = 0; }
--                     if (++n % 500000 == 0) System.out.println("..." + n + " " + (System.currentTimeMillis()-t0)/1000 + "s");
--                 }
--                 if (batch > 0) { ps.executeBatch(); dw.commit(); }
--                 System.out.println("[DONE] " + n + " rows");
--             }
--         }
--     }
-- }
-- ============================================================================
