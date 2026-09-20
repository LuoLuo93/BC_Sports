-- ==========================================================
-- 打印字段映射：产地 PlaceName 由固定值"中国"改为映射货品明细 origin 字段
-- 日期: 2026-09-20
--
-- 背景:
--   产地数据源接入 ERP M_PRODUCT.MADEIN(贴纸资料维护页/批量导入可编辑),
--   打印申请单明细新增 origin 列入库;模板字段 PlaceName(产地)原先
--   db_field 为 NULL + default_value='中国'(恒打印固定值)。
--
-- 变更: PlaceName 绑定 db_field=origin,默认值'中国'保留——
--   明细产地有值(货品维护过 MADEIN)打印真实产地;
--   为空(null 或空串,未维护)时由 PrintTaskService 回退默认值打印"中国"。
--
-- 注: 生产主库(192.168.5.177 bc_sports)已于 2026-09-20 直接执行生效,
--     本文件为变更存档,供其他环境/重建时复现。
-- ==========================================================

UPDATE print_field_mapping
   SET db_field = 'origin',
       update_time = SYSDATE
 WHERE template_field = 'PlaceName'
   AND db_field IS NULL;

COMMIT;

-- 验证: 应返回 db_field=origin, default_value=中国
SELECT db_field, template_field, default_value
  FROM print_field_mapping
 WHERE template_field = 'PlaceName';
